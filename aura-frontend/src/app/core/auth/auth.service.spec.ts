import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { APP_CONFIG, AppConfig } from '../../../app.config';
import { AuthService, AuthResponse } from './auth.service';

const appConfig: AppConfig = {
  apiBaseUrl: 'http://localhost',
  authUseCredentials: false
};

const authResponse: AuthResponse = {
  accessToken: 'token-123',
  tokenType: 'Bearer',
  user: { id: '1', email: 'user@example.com', username: 'user', role: 'USER' }
};

describe('AuthService', () => {
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    sessionStorage.clear();

    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: APP_CONFIG, useValue: appConfig }
      ]
    });

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl');
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('stores token and user on login', () => {
    // Arrange
    const service = TestBed.inject(AuthService);

    // Act
    service.login('user@example.com', 'password123').subscribe();
    const req = httpMock.expectOne('http://localhost/api/auth/login');
    req.flush(authResponse);

    // Assert
    expect(service.accessToken()).toBe('token-123');
    expect(service.user()?.email).toBe('user@example.com');
    expect(sessionStorage.getItem('aura.auth.token')).toBe('token-123');
  });

  it('clears auth on 401 from me()', () => {
    // Arrange
    sessionStorage.setItem('aura.auth.token', 'stale-token');
    const service = TestBed.inject(AuthService);

    // Act
    service.me().subscribe({ error: () => undefined });
    const req = httpMock.expectOne('http://localhost/api/auth/me');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    // Assert
    expect(service.hasToken()).toBeFalse();
    expect(sessionStorage.getItem('aura.auth.token')).toBeNull();
  });

  it('returns false from ensureAuthenticated when no token', () => {
    // Arrange
    const service = TestBed.inject(AuthService);
    let result: boolean | undefined;

    // Act
    service.ensureAuthenticated().subscribe(value => (result = value));

    // Assert
    expect(result).toBeFalse();
    httpMock.expectNone('http://localhost/api/auth/me');
  });
});
