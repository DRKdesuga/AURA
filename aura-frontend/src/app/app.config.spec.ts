import { TestBed } from '@angular/core/testing';
import { APP_CONFIG, AppConfig, provideAppConfig } from '../app.config';
import { environment } from '../environments/environment';

describe('app config', () => {
  it('provides APP_CONFIG from environment', () => {
    // Arrange
    TestBed.configureTestingModule({
      providers: [...provideAppConfig()]
    });

    // Act
    const cfg = TestBed.inject<AppConfig>(APP_CONFIG);

    // Assert
    expect(cfg.apiBaseUrl).toBe(environment.apiBaseUrl);
    expect(cfg.authUseCredentials).toBe(environment.authUseCredentials ?? false);
  });
});
