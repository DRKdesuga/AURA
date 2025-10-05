import {
  ApplicationConfig,
  InjectionToken,
  Provider,
  provideZonelessChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { routes } from './app.routes';
import { environment } from './environments/environment';

export interface AppConfig {
  apiBaseUrl: string;
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');

export function provideAppConfig(): Provider[] {
  const cfg: AppConfig = {
    apiBaseUrl: environment.apiBaseUrl
  };
  return [{ provide: APP_CONFIG, useValue: cfg }];
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withFetch()),
    ...provideAppConfig()
  ]
};
