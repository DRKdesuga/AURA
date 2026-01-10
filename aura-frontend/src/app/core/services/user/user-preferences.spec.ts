import { TestBed } from '@angular/core/testing';
import { UserPreferencesService } from './user-preferences';

describe('UserPreferencesService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('uses defaults when localStorage is empty', () => {
    // Arrange
    const service = TestBed.inject(UserPreferencesService);

    // Act
    const avatar = service.avatarUrl();
    const color = service.auraColor();

    // Assert
    expect(avatar).toBe('assets/user/default-avatar.png');
    expect(color).toBe('#6f9dff');
  });

  it('persists avatar and aura color', () => {
    // Arrange
    const service = TestBed.inject(UserPreferencesService);

    // Act
    service.setAvatar('assets/user/custom.png');
    service.setAuraColor('#112233');

    // Assert
    expect(service.avatarUrl()).toBe('assets/user/custom.png');
    expect(service.auraColor()).toBe('#112233');
    expect(localStorage.getItem('aura.avatar')).toBe('assets/user/custom.png');
    expect(localStorage.getItem('aura.auraColor')).toBe('#112233');
  });
});
