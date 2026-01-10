import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { UserAvatar } from './user-avatar';
import { UserPreferencesService } from '../../../core/services/user/user-preferences';

class PrefsStub {
  avatarUrl = signal('assets/user/default-avatar.png');
  auraColor = signal('#6f9dff');
}

describe('UserAvatar', () => {
  let fixture: ComponentFixture<UserAvatar>;
  let component: UserAvatar;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserAvatar],
      providers: [{ provide: UserPreferencesService, useClass: PrefsStub }]
    }).compileComponents();

    fixture = TestBed.createComponent(UserAvatar);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders a button when clickable is true', () => {
    // Arrange
    component.clickable = true;

    // Act
    fixture.detectChanges();

    // Assert
    const button = fixture.nativeElement.querySelector('button.avatar') as HTMLButtonElement;
    expect(button).toBeTruthy();
  });

  it('renders a read-only div when clickable is false', () => {
    // Arrange
    component.clickable = false;

    // Act
    fixture.detectChanges();

    // Assert
    const div = fixture.nativeElement.querySelector('div.avatar.ro') as HTMLDivElement;
    expect(div).toBeTruthy();
  });

  it('emits open when button is clicked', () => {
    // Arrange
    component.clickable = true;
    let emitted = false;
    component.open.subscribe(() => (emitted = true));

    // Act
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button.avatar') as HTMLButtonElement;
    button.click();

    // Assert
    expect(emitted).toBeTrue();
  });
});
