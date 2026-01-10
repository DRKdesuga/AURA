import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { ProfileModal } from './profile-modal';
import { UserPreferencesService } from '../../../core/services/user/user-preferences';
import { AuthService } from '../../../core/auth/auth.service';

class PrefsStub {
  avatarUrl = signal('assets/user/default-avatar.png');
  auraColor = signal('#6f9dff');
  setAvatar = jasmine.createSpy('setAvatar');
  setAuraColor = jasmine.createSpy('setAuraColor');
}

describe('ProfileModal', () => {
  let fixture: ComponentFixture<ProfileModal>;
  let component: ProfileModal;
  let auth: jasmine.SpyObj<AuthService>;
  let prefs: PrefsStub;

  beforeEach(async () => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['logout']);
    auth.logout.and.returnValue(of(void 0));

    prefs = new PrefsStub();

    await TestBed.configureTestingModule({
      imports: [ProfileModal],
      providers: [
        { provide: UserPreferencesService, useValue: prefs },
        { provide: AuthService, useValue: auth }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProfileModal);
    component = fixture.componentInstance;
    component.open = true;
    fixture.detectChanges();
  });

  it('saves preferences and emits close', () => {
    // Arrange
    let closed = false;
    component.close.subscribe(() => (closed = true));

    component.selected.set('assets/user/custom.png');
    component.color.set('#112233');

    // Act
    component.onSave();

    // Assert
    expect(prefs.setAvatar).toHaveBeenCalledWith('assets/user/custom.png');
    expect(prefs.setAuraColor).toHaveBeenCalledWith('#112233');
    expect(closed).toBeTrue();
  });

  it('does not logout when confirm is cancelled', () => {
    // Arrange
    spyOn(window, 'confirm').and.returnValue(false);

    // Act
    component.onLogout();

    // Assert
    expect(auth.logout).not.toHaveBeenCalled();
    expect(component.loggingOut()).toBeFalse();
  });

  it('calls logout when confirm is accepted', () => {
    // Arrange
    spyOn(window, 'confirm').and.returnValue(true);

    // Act
    component.onLogout();

    // Assert
    expect(auth.logout).toHaveBeenCalled();
    expect(component.loggingOut()).toBeFalse();
  });
});
