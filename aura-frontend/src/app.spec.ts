import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideZonelessChangeDetection()]
    }).compileComponents();
  });

  it('should create the app', () => {
    // Arrange
    const fixture = TestBed.createComponent(AppComponent);

    // Act
    const app = fixture.componentInstance;

    // Assert
    expect(app).toBeTruthy();
  });

  it('renders the router outlet', () => {
    // Arrange
    const fixture = TestBed.createComponent(AppComponent);

    // Act
    fixture.detectChanges();

    // Assert
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });
});
