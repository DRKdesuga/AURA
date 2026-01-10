import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { SessionsSidebar } from './sessions-sidebar';
import { SessionsService } from '../../../core/services/sessions/sessions';
import { SessionsPage } from '../../../core/models/sessions-page.model';

const mockPage: SessionsPage = {
  items: [
    { sessionId: 1, title: 'Cafe chat', preview: 'Hi', lastMessageAt: null, messageCount: 1 },
    { sessionId: 2, title: 'Caf\u00e9 update', preview: 'Yo', lastMessageAt: null, messageCount: 2 },
    { sessionId: 3, title: 'Other', preview: '', lastMessageAt: null, messageCount: 0 }
  ],
  total: 3,
  page: 0,
  size: 100
};

describe('SessionsSidebar', () => {
  let fixture: ComponentFixture<SessionsSidebar>;
  let component: SessionsSidebar;
  let sessions: jasmine.SpyObj<SessionsService>;

  beforeEach(async () => {
    sessions = jasmine.createSpyObj<SessionsService>('SessionsService', ['list']);
    sessions.list.and.returnValue(of(mockPage));

    await TestBed.configureTestingModule({
      imports: [SessionsSidebar],
      providers: [{ provide: SessionsService, useValue: sessions }]
    }).compileComponents();

    fixture = TestBed.createComponent(SessionsSidebar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads sessions on init', () => {
    // Arrange
    fixture.detectChanges();

    // Assert
    expect(sessions.list).toHaveBeenCalled();
    const items = fixture.nativeElement.querySelectorAll('app-session-card');
    expect(items.length).toBe(2);
  });

  it('filters sessions by normalized query', () => {
    // Arrange
    component.query.set('cafe');

    // Act
    fixture.detectChanges();

    // Assert
    const items = fixture.nativeElement.querySelectorAll('app-session-card');
    expect(items.length).toBe(2);
  });

  it('emits newDraft when create is clicked', () => {
    // Arrange
    let emitted = false;
    component.newDraft.subscribe(() => (emitted = true));

    // Act
    const button = fixture.nativeElement.querySelector('app-ui-button') as HTMLElement;
    button.dispatchEvent(new Event('click'));

    // Assert
    expect(emitted).toBeTrue();
  });

  it('refreshes when search enter is triggered', () => {
    // Arrange
    sessions.list.calls.reset();

    // Act
    component.onSearchEnter();

    // Assert
    expect(sessions.list).toHaveBeenCalled();
  });
});
