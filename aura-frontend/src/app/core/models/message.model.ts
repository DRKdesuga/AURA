export interface Message {
  id: number;
  author: 'USER' | 'ASSISTANT';
  content: string;
  timestamp: string;
}
