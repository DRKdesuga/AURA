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

  it('posts multipart form data when sending a file', () => {
    // Arrange
    const file = new File(['%PDF-1.4'], 'guide.pdf', { type: 'application/pdf' });
    const response = {
      sessionId: 2,
      userMessageId: 3,
      assistantMessageId: 4,
      assistantReply: 'Done',
      timestamp: 'now',
      newSession: false
    };

    // Act
    service.chatWithFile({ sessionId: 2, message: 'Hi', file }).subscribe();
    const req = httpMock.expectOne('http://localhost/api/chat/with-file');

    // Assert
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBeTrue();
    const body = req.request.body as FormData;
    expect(body.get('message')).toBe('Hi');
    expect(body.get('sessionId')).toBe('2');
    expect(body.get('file')).toBe(file);
    req.flush(response);
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
