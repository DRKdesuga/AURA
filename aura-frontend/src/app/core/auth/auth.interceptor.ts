import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { APP_CONFIG, AppConfig } from '../../../app.config';
import { AuthService } from './auth.service';

const AUTH_EXEMPT_PATHS = ['/api/auth/login', '/api/auth/register'];

function normalizeBaseUrl(apiBaseUrl: string): string {
  return apiBaseUrl.replace(/\/+$/, '');
}

function isApiRequest(url: string, apiBaseUrl: string): boolean {
  const base = normalizeBaseUrl(apiBaseUrl);
  return url.startsWith(`${base}/api/`);
}

function isAuthExempt(url: string): boolean {
  return AUTH_EXEMPT_PATHS.some(path => url.includes(path));
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const cfg = inject<AppConfig>(APP_CONFIG);
  const auth = inject(AuthService);

  const apiRequest = isApiRequest(req.url, cfg.apiBaseUrl);
  const authExempt = isAuthExempt(req.url);
  const logoutRequest = req.url.includes('/api/auth/logout');

  let authReq = req;
  if (apiRequest && !authExempt) {
    const token = auth.accessToken();
    if (token) {
      authReq = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }
  }

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (apiRequest && !authExempt && !logoutRequest && err.status === 401) {
        auth.logout({ skipServer: true }).subscribe();
      }
      return throwError(() => err);
    })
  );
};
