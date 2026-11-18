import { HttpClient, httpResource } from '@angular/common/http';
import { Injectable, Signal, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type DocumentType = 'SALES_AGREEMENT' | 'INVOICE' | 'COC' | 'OC_POLICY' | 'REGISTRATION_POA';

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  SALES_AGREEMENT: 'Sales agreement',
  INVOICE: 'Invoice',
  COC: 'Certificate of conformity (COC)',
  OC_POLICY: 'OC insurance policy',
  REGISTRATION_POA: 'Registration power of attorney',
};

export const DOCUMENT_TYPES = Object.keys(DOCUMENT_TYPE_LABELS) as DocumentType[];

export type DocumentStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export const DOCUMENT_STATUS_LABELS: Record<DocumentStatus, string> = {
  PENDING: 'Pending verification',
  VERIFIED: 'Verified',
  REJECTED: 'Rejected',
};

/** Status changes the backend accepts. A rejected document stays rejected; a correction is a new document. */
export const STATUS_ACTIONS: Record<DocumentStatus, { status: DocumentStatus; label: string }[]> = {
  PENDING: [
    { status: 'VERIFIED', label: 'Verify' },
    { status: 'REJECTED', label: 'Reject' },
  ],
  VERIFIED: [{ status: 'REJECTED', label: 'Reject' }],
  REJECTED: [],
};

export interface VehicleDocument {
  id: number;
  type: DocumentType;
  number: string;
  issueDate: string;
  expiryDate: string | null;
  status: DocumentStatus;
}

export type ReadinessReason = 'NOT_VERIFIED' | 'EXPIRED' | 'NO_HANDOVER_DATE';

export const READINESS_REASON_LABELS: Record<ReadinessReason, string> = {
  NOT_VERIFIED: 'not verified',
  EXPIRED: 'expires before the required date',
  NO_HANDOVER_DATE: 'No planned handover date',
};

export interface HandoverReadiness {
  ready: boolean;
  missingDocumentTypes: DocumentType[];
  problems: { documentType: DocumentType | null; reason: ReadinessReason }[];
}

export type VehicleDocumentRequest = Omit<VehicleDocument, 'id' | 'status'>;

@Injectable({ providedIn: 'root' })
export class DocumentApi {
  private readonly http = inject(HttpClient);

  private url(vehicleId: number): string {
    return `/api/vehicles/${vehicleId}/documents`;
  }

  list(vehicleId: Signal<number | undefined>) {
    return httpResource<VehicleDocument[]>(
      () => {
        const id = vehicleId();
        return id === undefined ? undefined : this.url(id);
      },
      { defaultValue: [] },
    );
  }

  readiness(vehicleId: Signal<number | undefined>) {
    return httpResource<HandoverReadiness>(() => {
      const id = vehicleId();
      return id === undefined ? undefined : `/api/vehicles/${id}/handover-readiness`;
    });
  }

  create(vehicleId: number, request: VehicleDocumentRequest): Observable<VehicleDocument> {
    return this.http.post<VehicleDocument>(this.url(vehicleId), request);
  }

  changeStatus(
    vehicleId: number,
    documentId: number,
    status: DocumentStatus,
  ): Observable<VehicleDocument> {
    return this.http.put<VehicleDocument>(`${this.url(vehicleId)}/${documentId}/status`, {
      status,
    });
  }
}
