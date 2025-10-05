import { Component } from '@angular/core';
import { AuraShell } from './app/features/layout/aura-shell/aura-shell';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [AuraShell],
  template: `<app-aura-shell></app-aura-shell>`,
  styleUrl: './app.scss'
})
export class AppComponent {}
