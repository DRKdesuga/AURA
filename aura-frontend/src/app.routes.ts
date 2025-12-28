import { Routes } from '@angular/router';
import { AuraShell } from './app/features/layout/aura-shell/aura-shell';
import { LoginComponent } from './app/features/auth/login/login.component';
import { authGuard } from './app/core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'app' },
  { path: 'login', component: LoginComponent },
  { path: 'app', component: AuraShell, canActivate: [authGuard] }
];
