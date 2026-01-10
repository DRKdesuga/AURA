import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { firstValueFrom, isObservable, of } from 'rxjs';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

const dummyRoutes = [{ path: 'login', component: class DummyComponent {} }];

describe('authGuard', () => {
  it('allows navigation when authenticated', async () => {
    // Arrange
    const auth = { ensureAuthenticated: () => of(true) } as Partial<AuthService>;

    TestBed.configureTestingModule({
      providers: [provideRouter(dummyRoutes), { provide: AuthService, useValue: auth }]
    });

    // Act
    const guardResult = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    const result = isObservable(guardResult)
      ? await firstValueFrom(guardResult)
      : await Promise.resolve(guardResult);

    // Assert
    expect(result).toBeTrue();
  });

  it('redirects to login when not authenticated', async () => {
    // Arrange
    const auth = { ensureAuthenticated: () => of(false) } as Partial<AuthService>;

    TestBed.configureTestingModule({
      providers: [provideRouter(dummyRoutes), { provide: AuthService, useValue: auth }]
    });

    const router = TestBed.inject(Router);

    // Act
    const guardResult = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    const result = isObservable(guardResult)
      ? await firstValueFrom(guardResult)
      : await Promise.resolve(guardResult);

    // Assert
    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });
});
