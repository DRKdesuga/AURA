import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { APP_CONFIG, AppConfig } from '../../../../app.config';
import { ChatService } from './chat';

const appConfig: AppConfig = {
  apiBaseUrl: 'http://localhost',
  authUseCredentials: false
};

describe('ChatService', () => {
  let service: ChatService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: APP_CONFIG, useValue: appConfig }
      ]
    });

    service = TestBed.inject(ChatService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('posts chat requests', () => {
    // Arrange
    const body = { sessionId: 4, message: 'Hello' };
    const response = { sessionId: 4, assistantMessageId: 99, assistantReply: 'Hi', timestamp: 'now' };

    // Act
    service.chat(body).subscribe();
    const req = httpMock.expectOne('http://localhost/api/chat');

    // Assert
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush(response);
  });

  it('surfaces errors for sessionExists', () => {
    // Arrange
    let errorStatus: number | undefined;

    // Act
    service.sessionExists(42).subscribe({
      error: err => (errorStatus = err.status)
    });

    const req = httpMock.expectOne('http://localhost/api/chat/session/42/exists');
    req.flush({ message: 'Missing' }, { status: 404, statusText: 'Not Found' });

    // Assert
    expect(errorStatus).toBe(404);
  });

  it('gets messages for a session', () => {
    // Arrange
    const response = [{ id: 1, author: 'USER', content: 'Hello', timestamp: 'now' }];

    // Act
    service.getMessages(7).subscribe();
    const req = httpMock.expectOne('http://localhost/api/chat/session/7/messages');

    // Assert
    expect(req.request.method).toBe('GET');
    req.flush(response);
  });
});
