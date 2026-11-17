import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { of, throwError } from 'rxjs';
import { Reports } from './reports';
import { ApiService } from '../../core/services/api/api.service';

describe('Reports', () => {
  let component: Reports;
  let fixture: ComponentFixture<Reports>;

  const mockApiService = {
    getKpis: vi.fn()
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    mockApiService.getKpis.mockReturnValue(
      of({
        labels: ['Lun', 'Mar', 'Mié'],
        rentalsPerDay: [4, 7, 9]
      })
    );

    await TestBed.configureTestingModule({
      imports: [Reports],
      providers: [
        provideHttpClient(),
        provideCharts(withDefaultRegisterables()),
        { provide: ApiService, useValue: mockApiService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Reports);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should load KPI chart data and keep fallback on error', () => {
    expect(component).toBeTruthy();
    expect(component.barChartData.labels).toEqual(['Lun', 'Mar', 'Mié']);
    expect(component.barChartData.datasets[0].data).toEqual([4, 7, 9]);

    mockApiService.getKpis.mockReturnValue(throwError(() => new Error('Reports API error')));
    component.ngOnInit();
    expect(component.kpiSummary.totalFleetUnits).toBe(36);
  });
});
