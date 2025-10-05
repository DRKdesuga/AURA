import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SessionsPage } from '../../models/sessions-page.model';
import { CreateSessionResponse } from '../../models/create-session-response.model';
import { UpdateSessionTitleRequest } from '../../models/update-session-title-request.model';
import { APP_CONFIG, AppConfig } from '../../../../app.config';

@Injectable({ providedIn: 'root' })
export class SessionsService {
  private readonly http = inject(HttpClient);
  private readonly cfg = inject<AppConfig>(APP_CONFIG);

  list(q = '', page = 0, size = 30): Observable<SessionsPage> {
    const params = new HttpParams().set('q', q).set('page', page).set('size', size);
    return this.http.get<SessionsPage>(`${this.cfg.apiBaseUrl}/api/sessions`, { params });
  }

  create(title?: string): Observable<CreateSessionResponse> {
    let params: HttpParams | undefined;
    if (title) params = new HttpParams().set('title', title);

    return this.http.post<CreateSessionResponse>(
      `${this.cfg.apiBaseUrl}/api/sessions`,
      {}, // body
      {
        params,
        observe: 'body',
        responseType: 'json'
      } as const
    );
  }

  updateTitle(id: number, body: UpdateSessionTitleRequest): Observable<void> {
    return this.http.patch<void>(
      `${this.cfg.apiBaseUrl}/api/sessions/${id}/title`,
      body,
      {
        observe: 'body',
        responseType: 'json'
      } as const
    );
  }
}
