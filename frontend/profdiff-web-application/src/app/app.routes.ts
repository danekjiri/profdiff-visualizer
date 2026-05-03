import { Routes } from '@angular/router';
import { CompareComponent } from './pages/compare/compare.component';
import { HomeComponent } from './pages/home/home.component';
import { ReportComponent } from './pages/report/report.component';

export const routes: Routes = [
  {
    path: '',
    component: HomeComponent,
  },
  {
    path: 'report',
    component: ReportComponent
  },
  {
    path: 'compare',
    component: CompareComponent
  },
  {
    path: '**',
    redirectTo: ''
  }
];
