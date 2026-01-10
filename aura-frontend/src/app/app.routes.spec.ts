import { Route } from '@angular/router';
import { routes } from '../app.routes';
import { AuraShell } from '../app/features/layout/aura-shell/aura-shell';
import { LoginComponent } from '../app/features/auth/login/login.component';
import { authGuard } from '../app/core/auth/auth.guard';

describe('app routes', () => {
  it('defines login and app routes', () => {
    // Arrange
    const loginRoute = routes.find((r: Route) => r.path === 'login');
    const appRoute = routes.find((r: Route) => r.path === 'app');

    // Act
    const loginComponent = loginRoute?.component;
    const appComponent = appRoute?.component;

    // Assert
    expect(loginComponent).toBe(LoginComponent);
    expect(appComponent).toBe(AuraShell);
    expect(appRoute?.canActivate).toEqual([authGuard]);
  });

  it('redirects empty path to /app', () => {
    // Arrange
    const rootRoute = routes.find((r: Route) => r.path === '');

    // Act
    const redirectTo = rootRoute?.redirectTo;

    // Assert
    expect(rootRoute?.pathMatch).toBe('full');
    expect(redirectTo).toBe('app');
  });
});
