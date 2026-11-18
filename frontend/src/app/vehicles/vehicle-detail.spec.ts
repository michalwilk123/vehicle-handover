import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { VehicleDetail } from './vehicle-detail';

const VEHICLE = {
  id: 3,
  vin: 'TMBJJ7NE8L0000003',
  make: 'Skoda',
  model: 'Octavia',
  productionYear: 2026,
  fuelType: 'HYBRID',
  plannedHandoverDate: null,
  dealershipRegisters: true,
};

const PENDING_COC = {
  id: 11,
  type: 'COC',
  number: 'COC-1',
  issueDate: '2026-01-10',
  expiryDate: null,
  status: 'PENDING',
};

describe('VehicleDetail', () => {
  let fixture: ComponentFixture<VehicleDetail>;
  let element: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(VehicleDetail);
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    fixture.componentRef.setInput('id', '3');
    fixture.detectChanges();
    http.expectOne({ method: 'GET', url: '/api/vehicles/3' }).flush(VEHICLE);
  });

  afterEach(() => http.verify());

  async function respond(url: string, body: object): Promise<void> {
    const request = await vi.waitFor(() => http.expectOne({ method: 'GET', url }));
    request.flush(body);
  }

  function readinessLines(): string[] {
    const section = element.querySelector('app-handover-readiness')!;
    return Array.from(section.querySelectorAll('p, li'), (line) => line.textContent!.trim());
  }

  it('shows the registration choice, missing documents and problems', async () => {
    await respond('/api/vehicles/3/handover-readiness', {
      ready: false,
      missingDocumentTypes: ['SALES_AGREEMENT', 'REGISTRATION_POA'],
      problems: [
        { documentType: 'COC', reason: 'NOT_VERIFIED' },
        { documentType: 'OC_POLICY', reason: 'EXPIRED' },
        { documentType: null, reason: 'NO_HANDOVER_DATE' },
      ],
    });
    await respond('/api/vehicles/3/documents', [PENDING_COC]);
    await fixture.whenStable();

    expect(element.textContent).toContain('Dealership registers');
    expect(readinessLines()).toEqual([
      'Not ready for handover',
      'Missing documents:',
      'Sales agreement',
      'Registration power of attorney',
      'Problems:',
      'Certificate of conformity (COC): not verified',
      'OC insurance policy: expires before the required date',
      'No planned handover date',
    ]);
  });

  it('refreshes readiness after verifying a document and undoing verification', async () => {
    await respond('/api/vehicles/3/handover-readiness', {
      ready: false,
      missingDocumentTypes: [],
      problems: [{ documentType: 'COC', reason: 'NOT_VERIFIED' }],
    });
    await respond('/api/vehicles/3/documents', [PENDING_COC]);
    await fixture.whenStable();

    element.querySelector<HTMLButtonElement>('tbody button')!.click();
    http
      .expectOne({ method: 'PUT', url: '/api/vehicles/3/documents/11/status' })
      .flush({ ...PENDING_COC, status: 'VERIFIED' });
    await respond('/api/vehicles/3/documents', [{ ...PENDING_COC, status: 'VERIFIED' }]);
    await respond('/api/vehicles/3/handover-readiness', {
      ready: true,
      missingDocumentTypes: [],
      problems: [],
    });

    await vi.waitFor(() => expect(readinessLines()).toEqual(['Ready for handover']));

    const undo = Array.from(element.querySelectorAll<HTMLButtonElement>('tbody button')).find(
      (button) => button.textContent.trim() === 'Undo verification',
    )!;
    undo.click();
    const request = http.expectOne({ method: 'PUT', url: '/api/vehicles/3/documents/11/status' });
    expect(request.request.body).toEqual({ status: 'PENDING' });
    request.flush(PENDING_COC);
    await respond('/api/vehicles/3/documents', [PENDING_COC]);
    await respond('/api/vehicles/3/handover-readiness', {
      ready: false,
      missingDocumentTypes: [],
      problems: [{ documentType: 'COC', reason: 'NOT_VERIFIED' }],
    });

    await vi.waitFor(() =>
      expect(readinessLines()).toEqual([
        'Not ready for handover',
        'Problems:',
        'Certificate of conformity (COC): not verified',
      ]),
    );
  });
});
