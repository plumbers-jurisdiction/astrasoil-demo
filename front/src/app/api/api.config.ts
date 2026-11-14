import { InjectionToken } from '@angular/core';

/** Override this token to change the backend without modifying the services. */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => 'https://astrasoil-back.onrender.com',
});
