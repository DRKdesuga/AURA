import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { APP_CONFIG, AppConfig } from '../../../app.config';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

const appConfig: AppConfig = {
  apiBaseUrl: 'http://localhost',
  authUseCredentials: false
};

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['logout', 'accessToken']);
    auth.accessToken.and.returnValue('token-123');
    auth.logout.and.returnValue(of(void 0));

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: APP_CONFIG, useValue: appConfig },
        { provide: AuthService, useValue: auth }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('adds Authorization header for API requests', () => {
    // Arrange
    const url = 'http://localhost/api/sessions';

    // Act
    http.get(url).subscribe();
    const req = httpMock.expectOne(url);

    // Assert
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-123');
    req.flush({});
  });

  it('skips auth header for login', () => {
    // Arrange
    const url = 'http://localhost/api/auth/login';

    // Act
    http.post(url, { email: 'a', password: 'b' }).subscribe();
    const req = httpMock.expectOne(url);

    // Assert
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });

  it('logs out on 401 from protected endpoints', () => {
    // Arrange
    const url = 'http://localhost/api/chat';

    // Act
    http.post(url, { message: 'Hello' }).subscribe({ error: () => undefined });
    const req = httpMock.expectOne(url);
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    // Assert
    expect(auth.logout).toHaveBeenCalledWith({ skipServer: true });
  });
});
