import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { APP_INITIALIZER, ErrorHandler } from '@angular/core';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { AuthService } from './app/services/auth.service';
import { GlobalErrorHandler } from './app/core/error-handler';

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideAnimations(),
    // A Dupla Auth adiciona o auth.interceptor dentro desta lista
    provideHttpClient(withInterceptors([])),
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    {
      provide: APP_INITIALIZER,
      useFactory: (auth: AuthService) => () => auth.ready,
      deps: [AuthService],
      multi: true,
    },
  ]
}).catch(err => console.error(err));