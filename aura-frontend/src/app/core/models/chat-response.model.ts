export interface ChatResponse {
  sessionId: number;
  userMessageId: number;
  assistantMessageId: number;
  assistantReply: string;
  timestamp: string;
  newSession: boolean;
}
