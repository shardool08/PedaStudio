/** Firestore: catalog/tlmResources */

export interface TlmResourceItem {
  id: string;
  label: string;
  emoji: string;
  imageUrl?: string;
}

export interface TlmCatalogDocument {
  resources: TlmResourceItem[];
  updatedAt?: string;
}

/** Firestore: catalog/flashcards/lessons/{safeLessonId} */

export interface FlashcardItem {
  word: string;
  emoji: string;
  meaningMr: string;
  meaningHi: string;
  meaningUr: string;
  type: string;
  imageUrl?: string;
}

export interface FlashcardLessonDocument {
  lessonId: string;
  title: string;
  cards: FlashcardItem[];
  updatedAt?: string;
}
