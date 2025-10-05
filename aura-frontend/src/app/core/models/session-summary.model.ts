export interface SessionSummary {
  sessionId: number;
  title: string | null;
  preview: string | null;
  lastMessageAt: string | null;
  messageCount: number;
}
