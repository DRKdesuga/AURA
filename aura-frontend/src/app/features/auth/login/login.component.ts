import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { NgxDarkVeilComponent } from '@omnedia/ngx-dark-veil';
import { NgxTypewriterComponent } from '@omnedia/ngx-typewriter';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgxDarkVeilComponent, NgxTypewriterComponent],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  readonly registerMode = signal(false);
  readonly pending = signal(false);
  readonly errorMessage = signal('');

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  private readonly passwordMatchValidator: ValidatorFn = (control: AbstractControl) => {
    if (!this.registerMode()) return null;
    const password = control.get('password')?.value as string | null;
    const confirm = control.get('confirmPassword')?.value as string | null;
    if (!password || !confirm) return null;
    return password === confirm ? null : { passwordMismatch: true };
  };

  readonly form = this.fb.group(
    {
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['']
    },
    { validators: [this.passwordMatchValidator] }
  );

  readonly emailCtrl = this.form.get('email') as FormControl;
  readonly passwordCtrl = this.form.get('password') as FormControl;
  readonly confirmCtrl = this.form.get('confirmPassword') as FormControl;

  constructor() {
    this.syncConfirmValidators();
    if (this.auth.isAuthenticated()) {
      void this.router.navigateByUrl('/app');
    }
  }

  toggleMode(): void {
    if (this.pending()) return;
    this.registerMode.update(value => !value);
    this.syncConfirmValidators();
    this.form.updateValueAndValidity({ emitEvent: false });
    this.errorMessage.set('');
  }

  onSubmit(): void {
    this.errorMessage.set('');
    const trimmedEmail = (this.emailCtrl.value as string | null)?.trim() ?? '';
    if (trimmedEmail !== this.emailCtrl.value) {
      this.emailCtrl.setValue(trimmedEmail, { emitEvent: false });
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const isRegister = this.registerMode();
    const email = trimmedEmail;
    const password = this.passwordCtrl.value as string;

    if (isRegister && this.form.errors?.['passwordMismatch']) {
      return;
    }

    this.pending.set(true);

    const request$ = isRegister
      ? this.auth.register(email, this.deriveUsername(email), password)
      : this.auth.login(email, password);

    request$
      .pipe(
        finalize(() => {
          this.pending.set(false);
        })
      )
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/app');
        },
        error: err => {
          this.errorMessage.set(this.toFriendlyError(err, isRegister));
        }
      });
  }

  private syncConfirmValidators(): void {
    if (this.registerMode()) {
      this.confirmCtrl.setValidators([Validators.required]);
    } else {
      this.confirmCtrl.clearValidators();
      this.confirmCtrl.setValue('');
    }
    this.confirmCtrl.updateValueAndValidity({ emitEvent: false });
  }

  private deriveUsername(email: string): string {
    const localPart = email.split('@')[0] || 'user';
    const cleaned = localPart.replace(/[^a-zA-Z0-9._-]/g, '').slice(0, 32);
    return cleaned || 'user';
  }

  private toFriendlyError(err: unknown, isRegister: boolean): string {
    const anyErr = err as { status?: number; error?: { message?: string } };
    const message = anyErr?.error?.message?.toLowerCase() || '';

    const emailAlreadyUsed =
      anyErr?.status === 409 ||
      (message.includes('email') &&
        (message.includes('already') || message.includes('exist') || message.includes('used')));

    if (isRegister && emailAlreadyUsed) {
      return 'This email is already in use.';
    }

    if (!isRegister && anyErr?.status === 401) {
      return 'Incorrect email or password.';
    }

    return 'Something went wrong. Please try again.';
  }
}
