import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { AuthService, AuthResponse } from '../../../core/auth/auth.service';
import { LoginComponent } from './login.component';

const authResponse: AuthResponse = {
  accessToken: 'token-123',
  tokenType: 'Bearer',
  user: { id: '1', email: 'user@example.com', username: 'user', role: 'USER' }
};

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let auth: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(async () => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['login', 'register', 'isAuthenticated']);
    auth.isAuthenticated.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, RouterTestingModule],
      providers: [{ provide: AuthService, useValue: auth }]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('submits login and navigates on success', fakeAsync(() => {
    // Arrange
    auth.login.and.returnValue(of(authResponse));
    const navSpy = spyOn(router, 'navigateByUrl');

    component.form.setValue({
      email: '  user@example.com  ',
      password: 'password123',
      confirmPassword: ''
    });

    // Act
    component.onSubmit();
    tick();
    fixture.detectChanges();

    // Assert
    expect(auth.login).toHaveBeenCalledWith('user@example.com', 'password123');
    expect(navSpy).toHaveBeenCalledWith('/app');
  }));

  it('shows friendly error message for invalid login', fakeAsync(() => {
    // Arrange
    auth.login.and.returnValue(throwError(() => ({ status: 401 })));

    component.form.setValue({
      email: 'user@example.com',
      password: 'password123',
      confirmPassword: ''
    });

    // Act
    component.onSubmit();
    tick();
    fixture.detectChanges();

    // Assert
    const status = fixture.nativeElement.querySelector('.status') as HTMLElement;
    expect(status?.textContent).toContain('Incorrect email or password.');
  }));

  it('blocks register when passwords mismatch', fakeAsync(() => {
    // Arrange
    auth.register.and.returnValue(of(authResponse));

    component.toggleMode();
    component.form.setValue({
      email: 'user@example.com',
      password: 'password123',
      confirmPassword: 'password456'
    });

    // Act
    component.onSubmit();
    tick();
    fixture.detectChanges();

    // Assert
    expect(auth.register).not.toHaveBeenCalled();
    const error = fixture.nativeElement.querySelector('.error span') as HTMLElement;
    expect(error?.textContent).toContain('Passwords do not match.');
  }));
});
