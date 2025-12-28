import {
  ApplicationConfig,
  InjectionToken,
  Provider,
  provideZonelessChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { environment } from './environments/environment';
import { authInterceptor } from './app/core/auth/auth.interceptor';

export interface AppConfig {
  apiBaseUrl: string;
  authUseCredentials: boolean;
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');

export function provideAppConfig(): Provider[] {
  const cfg: AppConfig = {
    apiBaseUrl: environment.apiBaseUrl,
    authUseCredentials: environment.authUseCredentials ?? false
  };
  return [{ provide: APP_CONFIG, useValue: cfg }];
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
    ...provideAppConfig()
  ]
};
