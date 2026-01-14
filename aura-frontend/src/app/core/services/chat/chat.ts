import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatRequest } from '../../models/chat-request.model';
import { ChatResponse } from '../../models/chat-response.model';
import { Message } from '../../models/message.model';
import { APP_CONFIG, AppConfig } from '../../../../app.config';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly cfg = inject<AppConfig>(APP_CONFIG);

  chat(body: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.cfg.apiBaseUrl}/api/chat`, body);
  }

  chatWithFile(payload: { message: string; sessionId: number | null; file: File | null }): Observable<ChatResponse> {
    const formData = new FormData();
    formData.append('message', payload.message);
    if (payload.sessionId != null) formData.append('sessionId', String(payload.sessionId));
    if (payload.file) formData.append('file', payload.file);
    return this.http.post<ChatResponse>(`${this.cfg.apiBaseUrl}/api/chat/with-file`, formData);
  }

  getMessages(sessionId: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.cfg.apiBaseUrl}/api/chat/session/${sessionId}/messages`);
  }

  sessionExists(sessionId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.cfg.apiBaseUrl}/api/chat/session/${sessionId}/exists`);
  }
}
