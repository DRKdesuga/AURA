import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { ChatHeader } from './chat-header';
import { UserAvatar } from '../../../shared/components/user-avatar/user-avatar';
import { ProfileModal } from '../../../shared/components/profile-modal/profile-modal';
import { UserPreferencesService } from '../../../core/services/user/user-preferences';
import { AuthService } from '../../../core/auth/auth.service';

class PrefsStub {
  avatarUrl = signal('assets/user/default-avatar.png');
  auraColor = signal('#6f9dff');
  setAvatar = jasmine.createSpy('setAvatar');
  setAuraColor = jasmine.createSpy('setAuraColor');
}

describe('ChatHeader', () => {
  let fixture: ComponentFixture<ChatHeader>;
  let component: ChatHeader;

  beforeEach(async () => {
    const auth = jasmine.createSpyObj<AuthService>('AuthService', ['logout']);
    auth.logout.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [ChatHeader],
      providers: [
        { provide: UserPreferencesService, useClass: PrefsStub },
        { provide: AuthService, useValue: auth }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ChatHeader);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('emits toggleSidebar when button is clicked', () => {
    // Arrange
    let emitted = false;
    component.toggleSidebar.subscribe(() => (emitted = true));

    const button = fixture.nativeElement.querySelector('button.collapse-btn') as HTMLButtonElement;

    // Act
    button.click();

    // Assert
    expect(emitted).toBeTrue();
  });

  it('opens profile modal when avatar emits open', () => {
    // Arrange
    const avatar = fixture.debugElement.query(By.directive(UserAvatar));

    // Act
    avatar.triggerEventHandler('open', null);
    fixture.detectChanges();

    // Assert
    const modal = fixture.debugElement.query(By.directive(ProfileModal));
    expect(modal.componentInstance.open).toBeTrue();
  });
});
