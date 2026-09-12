import fs from "fs";

const t = fs.readFileSync(".env", "utf8");
const m = t.match(/^ANTHROPIC_API_KEY=(.*)$/m);
const v = m ? m[1].trim().replace(/^["']|["']$/g, "") : "";
if (!v) console.log("LOCAL_KEY: MISSING");
else console.log(`LOCAL_KEY: SET len=${v.length} starts=${v.slice(0, 7)}`);
