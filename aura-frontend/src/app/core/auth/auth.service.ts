import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { catchError, finalize, map, tap } from 'rxjs/operators';
import { APP_CONFIG, AppConfig } from '../../../app.config';

export interface AuthUser {
  id: string;
  email: string;
  username: string;
  role: 'USER' | 'ADMIN' | string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  user: AuthUser;
  refreshToken?: string;
}

interface AuthState {
  token: string | null;
  user: AuthUser | null;
  validated: boolean;
  refreshToken: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly cfg = inject<AppConfig>(APP_CONFIG);
  private readonly router = inject(Router);

  private readonly storageKey = 'aura.auth.token';
  private readonly refreshStorageKey = 'aura.auth.refresh';
  private readonly useCredentials = this.cfg.authUseCredentials ?? false;
  private readonly state = signal<AuthState>(this.loadInitialState());
  private meCheck$?: Observable<boolean>;

  readonly authState = this.state.asReadonly();
  readonly user = computed(() => this.state().user);
  readonly accessToken = computed(() => this.state().token);

  login(email: string, password: string): Observable<AuthResponse> {
    const options = this.useCredentials ? { withCredentials: true } : {};
    return this.http
      .post<AuthResponse>(`${this.cfg.apiBaseUrl}/api/auth/login`, { email, password }, options)
      .pipe(tap(res => this.setAuth(res)));
  }

  register(email: string, username: string, password: string): Observable<AuthResponse> {
    const options = this.useCredentials ? { withCredentials: true } : {};
    return this.http
      .post<AuthResponse>(`${this.cfg.apiBaseUrl}/api/auth/register`, { email, username, password }, options)
      .pipe(tap(res => this.setAuth(res)));
  }

  me(): Observable<AuthUser> {
    return this.http.get<AuthUser>(`${this.cfg.apiBaseUrl}/api/auth/me`).pipe(
      tap(user => this.setUser(user)),
      catchError(err => {
        if (err?.status === 401) this.clearAuth();
        return throwError(() => err);
      })
    );
  }

  logout(opts?: { skipServer?: boolean }): Observable<void> {
    const skipServer = opts?.skipServer ?? false;
    const refreshToken = this.state().refreshToken;

    const body = refreshToken ? { refreshToken } : {};
    const requestOptions = this.useCredentials ? { withCredentials: true } : {};
    const request$: Observable<void> = skipServer
      ? of(void 0)
      : this.http.post<void>(`${this.cfg.apiBaseUrl}/api/auth/logout`, body, requestOptions);

    return request$.pipe(
      catchError(() => of(void 0)),
      finalize(() => {
        this.clearAuth();
        void this.router.navigateByUrl('/login');
      }),
      map(() => void 0)
    );
  }

  clearAuth(): void {
    this.writeTokenToStorage(null);
    this.writeRefreshTokenToStorage(null);
    this.state.set({ token: null, user: null, validated: false, refreshToken: null });
  }

  hasToken(): boolean {
    return !!this.state().token;
  }

  isAuthenticated(): boolean {
    const current = this.state();
    return !!current.token && !!current.user;
  }

  ensureAuthenticated(): Observable<boolean> {
    const current = this.state();
    if (!current.token) return of(false);
    if (current.user) return of(true);
    if (current.validated) return of(false);

    if (this.meCheck$) return this.meCheck$;

    this.meCheck$ = this.me().pipe(
      map(() => true),
      catchError(() => {
        const latest = this.state();
        if (latest.token && !latest.validated) {
          this.state.set({
            token: latest.token,
            user: null,
            validated: true,
            refreshToken: latest.refreshToken
          });
        }
        return of(false);
      }),
      finalize(() => {
        this.meCheck$ = undefined;
      })
    );

    return this.meCheck$;
  }

  private setAuth(res: AuthResponse): void {
    this.writeTokenToStorage(res.accessToken);
    const refreshToken = res.refreshToken ?? null;
    this.writeRefreshTokenToStorage(refreshToken);
    this.state.set({
      token: res.accessToken,
      user: res.user,
      validated: true,
      refreshToken
    });
  }

  private setUser(user: AuthUser): void {
    const current = this.state();
    this.state.set({
      token: current.token,
      user,
      validated: true,
      refreshToken: current.refreshToken
    });
  }

  private loadInitialState(): AuthState {
    return {
      token: this.readTokenFromStorage(),
      user: null,
      validated: false,
      refreshToken: this.readRefreshTokenFromStorage()
    };
  }

  private readTokenFromStorage(): string | null {
    try {
      return sessionStorage.getItem(this.storageKey);
    } catch {
      return null;
    }
  }

  private writeTokenToStorage(token: string | null): void {
    try {
      if (token) {
        sessionStorage.setItem(this.storageKey, token);
      } else {
        sessionStorage.removeItem(this.storageKey);
      }
    } catch {
      // Ignore storage errors (e.g. disabled storage).
    }
  }

  private readRefreshTokenFromStorage(): string | null {
    try {
      return sessionStorage.getItem(this.refreshStorageKey);
    } catch {
      return null;
    }
  }

  private writeRefreshTokenToStorage(token: string | null): void {
    try {
      if (token) {
        sessionStorage.setItem(this.refreshStorageKey, token);
      } else {
        sessionStorage.removeItem(this.refreshStorageKey);
      }
    } catch {
      // Ignore storage errors (e.g. disabled storage).
    }
  }
}
