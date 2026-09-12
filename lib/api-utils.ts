import { NextResponse } from "next/server";

/** Tried in order until one works. Override first via ANTHROPIC_MODEL. */
export const ANTHROPIC_MODEL_FALLBACKS = [
  "claude-sonnet-4-5-20250929",
  "claude-sonnet-4-6",
  "claude-3-5-sonnet-20241022",
] as const;

export const DEFAULT_ANTHROPIC_MODEL = ANTHROPIC_MODEL_FALLBACKS[0];

export function requireEnv(name: string): string | null {
  const value = process.env[name];
  return value && value.length > 0 ? value : null;
}

export function getAnthropicModelCandidates(): string[] {
  const configured = requireEnv("ANTHROPIC_MODEL");
  const fallbacks = [...ANTHROPIC_MODEL_FALLBACKS];
  if (configured && !fallbacks.includes(configured as (typeof fallbacks)[number])) {
    return [configured, ...fallbacks];
  }
  if (configured) {
    return [configured, ...fallbacks.filter((m) => m !== configured)];
  }
  return fallbacks;
}

function anthropicErrorMessage(data: unknown, model: string): string {
  if (!data || typeof data !== "object") {
    return `AI service error (model: ${model})`;
  }
  const err = (data as { error?: unknown }).error;
  if (typeof err === "string" && err.trim()) return `${err} (model: ${model})`;
  if (err && typeof err === "object") {
    const msg = (err as { message?: string }).message;
    if (msg?.trim()) return `${msg} (model: ${model})`;
    const type = (err as { type?: string }).type;
    if (type?.trim()) return `${type} (model: ${model})`;
  }
  return `AI service error (model: ${model})`;
}

function isModelNotFound(status: number, data: unknown): boolean {
  if (status === 404) return true;
  if (!data || typeof data !== "object") return false;
  const err = (data as { error?: { type?: string; message?: string } }).error;
  const type = err?.type?.toLowerCase() ?? "";
  const msg = err?.message?.toLowerCase() ?? "";
  return type.includes("not_found") || msg.includes("model") || msg.includes("not found");
}

export async function anthropicMessages(body: Record<string, unknown>) {
  const apiKey = requireEnv("ANTHROPIC_API_KEY");
  if (!apiKey) {
    return {
      ok: false as const,
      response: NextResponse.json({ error: "API key not configured" }, { status: 503 }),
    };
  }

  // Ignore a hardcoded stale model from callers; we pick a working one.
  const { model: _ignored, ...rest } = body;
  const models = getAnthropicModelCandidates();
  let lastStatus = 502;
  let lastError = "AI service error";
  let lastModel = models[0] ?? DEFAULT_ANTHROPIC_MODEL;

  for (let i = 0; i < models.length; i++) {
    const model = models[i]!;
    lastModel = model;
    const payload = { ...rest, model };

    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": apiKey,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify(payload),
      signal: AbortSignal.timeout(120_000),
    });

    let data: unknown;
    try {
      data = await response.json();
    } catch {
      return {
        ok: false as const,
        response: NextResponse.json({ error: "Invalid response from AI service" }, { status: 502 }),
      };
    }

    if (response.ok && !(data && typeof data === "object" && "error" in data)) {
      if (i > 0) {
        console.warn(`anthropicMessages: succeeded with fallback model ${model}`);
      }
      return { ok: true as const, data: data as { content?: Array<{ text?: string }> } };
    }

    lastStatus = response.status >= 400 ? response.status : 502;
    lastError = anthropicErrorMessage(data, model);
    console.error(`anthropicMessages: model ${model} failed (${lastStatus}):`, data);

    const hasAnotherModel = i < models.length - 1;
    if (hasAnotherModel && isModelNotFound(lastStatus, data)) {
      continue;
    }
    break;
  }

  return {
    ok: false as const,
    response: NextResponse.json({ error: lastError, model: lastModel }, { status: lastStatus }),
  };
}

export function parseLessonGrade(lessonId: string): number {
  const match = lessonId.match(/^L?\d+/);
  if (!match) return 1;
  const digits = match[0].replace(/\D/g, "");
  return parseInt(digits, 10) || 1;
}
