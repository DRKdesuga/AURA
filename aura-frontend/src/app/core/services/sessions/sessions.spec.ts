import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { APP_CONFIG, AppConfig } from '../../../../app.config';
import { SessionsService } from './sessions';

const appConfig: AppConfig = {
  apiBaseUrl: 'http://localhost',
  authUseCredentials: false
};

describe('SessionsService', () => {
  let service: SessionsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: APP_CONFIG, useValue: appConfig }
      ]
    });

    service = TestBed.inject(SessionsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('lists sessions with query params', () => {
    // Arrange
    const mockResponse = { items: [], total: 0, page: 2, size: 10 };

    // Act
    service.list('hello', 2, 10).subscribe();
    const req = httpMock.expectOne(r => r.method === 'GET' && r.url === 'http://localhost/api/sessions');

    // Assert
    expect(req.request.params.get('q')).toBe('hello');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    req.flush(mockResponse);
  });

  it('creates a session without title when omitted', () => {
    // Arrange
    const mockResponse = { sessionId: 10, title: 'New chat' };

    // Act
    service.create().subscribe();
    const req = httpMock.expectOne('http://localhost/api/sessions');

    // Assert
    expect(req.request.method).toBe('POST');
    expect(req.request.params.has('title')).toBeFalse();
    req.flush(mockResponse);
  });

  it('surfaces errors from updateTitle', () => {
    // Arrange
    let errorStatus: number | undefined;

    // Act
    service.updateTitle(5, { title: 'New title' }).subscribe({
      error: err => (errorStatus = err.status)
    });

    const req = httpMock.expectOne('http://localhost/api/sessions/5/title');
    req.flush({ message: 'Failure' }, { status: 500, statusText: 'Server Error' });

    // Assert
    expect(errorStatus).toBe(500);
  });
});
