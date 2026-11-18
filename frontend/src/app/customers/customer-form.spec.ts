import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { CustomerForm } from './customer-form';

describe('CustomerForm', () => {
  let fixture: ComponentFixture<CustomerForm>;
  let element: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(CustomerForm);
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  // Inputs in template order: first name, last name, email, phone, NIP.
  function fill(index: number, value: string): void {
    const input = element.querySelectorAll<HTMLInputElement>('input')[index];
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function fillValidCustomer(): void {
    fill(0, 'Jan');
    fill(1, 'Kowalski');
    fill(2, 'jan.kowalski@example.com');
    fill(3, '+48 600 100 200');
  }

  async function submit(): Promise<void> {
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  function errors(): string[] {
    return Array.from(element.querySelectorAll('.error'), (error) => error.textContent!.trim());
  }

  it('shows required errors and sends nothing when the form is empty', async () => {
    await submit();

    http.expectNone('/api/customers');
    expect(errors()).toEqual([
      'First name is required',
      'Last name is required',
      'Email is required',
      'Phone is required',
    ]);
  });

  it('creates a customer without NIP as null and navigates to its details', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fillValidCustomer();
    fill(4, '   ');

    await submit();
    const request = http.expectOne({ method: 'POST', url: '/api/customers' });
    expect(request.request.body).toEqual({
      firstName: 'Jan',
      lastName: 'Kowalski',
      email: 'jan.kowalski@example.com',
      phone: '+48 600 100 200',
      nip: null,
    });
    request.flush({ id: 5, ...request.request.body });

    await vi.waitFor(() => expect(navigate).toHaveBeenCalledWith(['/customers', 5]));
  });

  it('loads the customer in edit mode and updates it with its NIP', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const customer = {
      id: 3,
      firstName: 'Anna',
      lastName: 'Nowak',
      email: 'Anna.Nowak@Example.com',
      phone: '600-100-200',
      nip: '1234567890',
    };
    fixture.componentRef.setInput('id', '3');
    fixture.detectChanges();
    http.expectOne({ method: 'GET', url: '/api/customers/3' }).flush(customer);
    await fixture.whenStable();
    fill(1, 'Kowalska');

    await submit();
    const request = http.expectOne({ method: 'PUT', url: '/api/customers/3' });
    expect(request.request.body).toEqual({
      firstName: 'Anna',
      lastName: 'Kowalska',
      email: 'Anna.Nowak@Example.com',
      phone: '600-100-200',
      nip: '1234567890',
    });
    request.flush({ ...customer, lastName: 'Kowalska' });

    await vi.waitFor(() => expect(navigate).toHaveBeenCalledWith(['/customers', 3]));
  });

  it('shows a dedicated message when the customer to edit does not exist', async () => {
    fixture.componentRef.setInput('id', '99');
    fixture.detectChanges();
    http
      .expectOne({ method: 'GET', url: '/api/customers/99' })
      .flush(
        { title: 'Not Found', status: 404, detail: 'Customer with id 99 not found' },
        { status: 404, statusText: 'Not Found' },
      );

    await vi.waitFor(() => expect(errors()).toEqual(['Customer not found']));
  });

  it('shows field errors returned by the backend at their inputs', async () => {
    fillValidCustomer();
    fill(4, '123');

    await submit();
    http.expectOne({ method: 'POST', url: '/api/customers' }).flush(
      {
        title: 'Bad Request',
        status: 400,
        detail: 'Validation failed',
        errors: [
          { field: 'email', message: 'must be a well-formed email address' },
          { field: 'nip', message: 'must be 10 digits' },
        ],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    await vi.waitFor(() =>
      expect(errors()).toEqual(['must be a well-formed email address', 'must be 10 digits']),
    );
  });

  it('shows the ProblemDetail detail when the email already exists', async () => {
    fillValidCustomer();

    await submit();
    http.expectOne({ method: 'POST', url: '/api/customers' }).flush(
      {
        title: 'Conflict',
        status: 409,
        detail: 'Customer with email jan.kowalski@example.com already exists',
      },
      { status: 409, statusText: 'Conflict' },
    );

    await vi.waitFor(() =>
      expect(errors()).toEqual(['Customer with email jan.kowalski@example.com already exists']),
    );
  });
});
