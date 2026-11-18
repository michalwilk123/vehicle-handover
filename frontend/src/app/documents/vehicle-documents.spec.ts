import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VehicleDocuments } from './vehicle-documents';

const PENDING_COC = {
  id: 11,
  type: 'COC',
  number: 'COC-1',
  issueDate: '2026-01-10',
  expiryDate: null,
  status: 'PENDING',
};

describe('VehicleDocuments', () => {
  let fixture: ComponentFixture<VehicleDocuments>;
  let element: HTMLElement;
  let http: HttpTestingController;
  let changes: number;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(VehicleDocuments);
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    changes = 0;
    fixture.componentInstance.changed.subscribe(() => changes++);
    fixture.componentRef.setInput('vehicleId', 4);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  async function loadDocuments(documents: unknown[]): Promise<void> {
    // A reload is requested asynchronously, so wait until the request is sent.
    const request = await vi.waitFor(() =>
      http.expectOne({ method: 'GET', url: '/api/vehicles/4/documents' }),
    );
    request.flush(documents);
    await fixture.whenStable();
  }

  function fill(selector: string, value: string): void {
    const input = element.querySelector<HTMLInputElement | HTMLSelectElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function dateInput(index: number): string {
    return `label:nth-of-type(${index}) input[type="date"]`;
  }

  async function submit(): Promise<void> {
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  function rows(): string[][] {
    return Array.from(element.querySelectorAll('tbody tr'), (row) =>
      Array.from(row.querySelectorAll('td'), (cell) => cell.textContent!.trim()),
    );
  }

  function errors(): string[] {
    return Array.from(element.querySelectorAll('.error'), (error) => error.textContent!.trim());
  }

  function button(label: string): HTMLButtonElement {
    return Array.from(element.querySelectorAll<HTMLButtonElement>('tbody button')).find(
      (candidate) => candidate.textContent!.trim() === label,
    )!;
  }

  it('lists documents with labels, including rejected ones', async () => {
    await loadDocuments([
      { ...PENDING_COC, id: 10, status: 'REJECTED' },
      { ...PENDING_COC, expiryDate: '2027-01-01' },
    ]);

    expect(rows().map((row) => row.slice(0, 5))).toEqual([
      ['Certificate of conformity (COC)', 'COC-1', '2026-01-10', '—', 'Rejected'],
      [
        'Certificate of conformity (COC)',
        'COC-1',
        '2026-01-10',
        '2027-01-01',
        'Pending verification',
      ],
    ]);
  });

  it('offers only the status changes the backend allows', async () => {
    await loadDocuments([
      PENDING_COC,
      { ...PENDING_COC, id: 12, status: 'VERIFIED' },
      { ...PENDING_COC, id: 13, status: 'REJECTED' },
    ]);

    expect(rows().map((row) => row[5].replace(/\s+/g, ' '))).toEqual([
      'Verify Reject',
      'Reject',
      '',
    ]);
  });

  it('shows the empty state', async () => {
    await loadDocuments([]);

    expect(rows()).toEqual([['No documents yet.']]);
  });

  it('adds a document without expiry date, reloads the list and reports the change', async () => {
    await loadDocuments([]);
    fill('select', 'COC');
    fill('input[type="text"]', 'COC-1');
    fill(dateInput(3), '2026-01-10');

    await submit();
    const request = http.expectOne({ method: 'POST', url: '/api/vehicles/4/documents' });
    expect(request.request.body).toEqual({
      type: 'COC',
      number: 'COC-1',
      issueDate: '2026-01-10',
      expiryDate: null,
    });
    request.flush(PENDING_COC);
    await loadDocuments([PENDING_COC]);

    expect(rows()[0][4]).toBe('Pending verification');
    expect(element.querySelector<HTMLInputElement>('input[type="text"]')!.value).toBe('');
    expect(changes).toBe(1);
  });

  it('sends nothing and shows required errors when the form is empty', async () => {
    await loadDocuments([]);

    await submit();

    http.expectNone('/api/vehicles/4/documents');
    expect(errors()).toEqual(['Type is required', 'Number is required', 'Issue date is required']);
  });

  it('shows a conflict from the backend above the form', async () => {
    await loadDocuments([PENDING_COC]);
    fill('select', 'COC');
    fill('input[type="text"]', 'COC-2');
    fill(dateInput(3), '2026-01-10');

    await submit();
    http
      .expectOne({ method: 'POST', url: '/api/vehicles/4/documents' })
      .flush(
        { title: 'Conflict', status: 409, detail: 'Vehicle 4 already has an active COC document' },
        { status: 409, statusText: 'Conflict' },
      );

    await vi.waitFor(() =>
      expect(errors()).toEqual(['Vehicle 4 already has an active COC document']),
    );
    expect(changes).toBe(0);
  });

  it('shows a field error for a future issue date at its input', async () => {
    await loadDocuments([]);
    fill('select', 'COC');
    fill('input[type="text"]', 'COC-1');
    fill(dateInput(3), '2999-01-01');

    await submit();
    http.expectOne({ method: 'POST', url: '/api/vehicles/4/documents' }).flush(
      {
        title: 'Bad Request',
        status: 400,
        detail: 'Validation failed',
        errors: [{ field: 'issueDate', message: 'must not be after today' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    await vi.waitFor(() => expect(errors()).toEqual(['must not be after today']));
  });

  it('changes the status, reloads the list and reports the change', async () => {
    await loadDocuments([PENDING_COC]);

    button('Verify').click();
    const request = http.expectOne({ method: 'PUT', url: '/api/vehicles/4/documents/11/status' });
    expect(request.request.body).toEqual({ status: 'VERIFIED' });
    request.flush({ ...PENDING_COC, status: 'VERIFIED' });
    await loadDocuments([{ ...PENDING_COC, status: 'VERIFIED' }]);

    expect(rows()[0][4]).toBe('Verified');
    expect(changes).toBe(1);
  });

  it('lets staff undo a staged rejection and sends it only on confirmation', async () => {
    const verified = { ...PENDING_COC, status: 'VERIFIED' };
    await loadDocuments([verified]);

    button('Reject').click();
    fixture.detectChanges();
    http.expectNone({ method: 'PUT', url: '/api/vehicles/4/documents/11/status' });
    expect(rows()[0][4]).toBe('Verified');

    button('Undo').click();
    fixture.detectChanges();
    http.expectNone({ method: 'PUT', url: '/api/vehicles/4/documents/11/status' });
    expect(button('Reject')).toBeDefined();
    expect(changes).toBe(0);

    button('Reject').click();
    fixture.detectChanges();
    button('Confirm rejection').click();
    const request = http.expectOne({ method: 'PUT', url: '/api/vehicles/4/documents/11/status' });
    expect(request.request.body).toEqual({ status: 'REJECTED' });
    const rejected = { ...verified, status: 'REJECTED' };
    request.flush(rejected);
    await loadDocuments([rejected]);

    expect(rows()[0][4]).toBe('Rejected');
    expect(changes).toBe(1);
  });

  it('shows a status change error until a document save succeeds', async () => {
    await loadDocuments([PENDING_COC]);

    button('Reject').click();
    fixture.detectChanges();
    button('Confirm rejection').click();
    http
      .expectOne({ method: 'PUT', url: '/api/vehicles/4/documents/11/status' })
      .flush(
        { title: 'Conflict', status: 409, detail: 'Status change not allowed' },
        { status: 409, statusText: 'Conflict' },
      );

    await vi.waitFor(() => expect(errors()).toEqual(['Status change not allowed']));
    expect(changes).toBe(0);

    fill('select', 'INVOICE');
    fill('input[type="text"]', 'Invoice 1');
    fill(dateInput(3), '2026-01-10');
    await submit();
    const invoice = { ...PENDING_COC, id: 12, type: 'INVOICE', number: 'Invoice 1' };
    http.expectOne({ method: 'POST', url: '/api/vehicles/4/documents' }).flush(invoice);
    await loadDocuments([PENDING_COC, invoice]);

    expect(errors()).toEqual([]);
    expect(changes).toBe(1);
  });
});
