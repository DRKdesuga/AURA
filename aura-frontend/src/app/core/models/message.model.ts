export interface MessageAttachment {
  name: string;
  size: number;
  type: string;
}

export interface Message {
  id: number;
  author: 'USER' | 'ASSISTANT';
  content: string;
  timestamp: string;
  attachments?: MessageAttachment[];
}
