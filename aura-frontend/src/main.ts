import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './ss/app.config';
import { App } from './ss/app';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
