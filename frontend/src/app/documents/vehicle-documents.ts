import { Component, inject, input, numberAttribute, output, signal } from '@angular/core';
import { FormField, FormRoot, form, required } from '@angular/forms/signals';
import { firstValueFrom } from 'rxjs';
import {
  loadErrorMessage,
  problemDetailMessage,
  problemDetailToFormErrors,
} from '../shared/problem-detail';
import {
  DOCUMENT_STATUS_LABELS,
  DOCUMENT_TYPES,
  DOCUMENT_TYPE_LABELS,
  DocumentApi,
  DocumentStatus,
  DocumentType,
  STATUS_ACTIONS,
  VehicleDocument,
  VehicleDocumentRequest,
} from './document-api';

interface DocumentFormModel {
  type: DocumentType | '';
  number: string;
  issueDate: string;
  expiryDate: string;
}

function emptyModel(): DocumentFormModel {
  return { type: '', number: '', issueDate: '', expiryDate: '' };
}

function toRequest(model: DocumentFormModel): VehicleDocumentRequest {
  return {
    type: model.type as DocumentType,
    number: model.number,
    issueDate: model.issueDate,
    expiryDate: model.expiryDate || null,
  };
}

@Component({
  selector: 'app-vehicle-documents',
  imports: [FormField, FormRoot],
  templateUrl: './vehicle-documents.html',
})
export class VehicleDocuments {
  private readonly documentApi = inject(DocumentApi);

  readonly vehicleId = input.required({ transform: numberAttribute });
  /** Emits after a document is added or its status changes. */
  readonly changed = output<void>();

  protected readonly documents = this.documentApi.list(this.vehicleId);
  protected readonly documentTypes = DOCUMENT_TYPES;
  protected readonly typeLabels = DOCUMENT_TYPE_LABELS;
  protected readonly statusLabels = DOCUMENT_STATUS_LABELS;
  protected readonly statusActions = STATUS_ACTIONS;
  protected readonly statusError = signal<string | null>(null);

  private readonly model = signal(emptyModel());

  protected readonly documentForm = form(
    this.model,
    (path) => {
      required(path.type, { message: 'Type is required' });
      required(path.number, { message: 'Number is required' });
      required(path.issueDate, { message: 'Issue date is required' });
    },
    {
      submission: {
        action: async (documentForm) => {
          try {
            await firstValueFrom(
              this.documentApi.create(this.vehicleId(), toRequest(documentForm().value())),
            );
            documentForm().reset(emptyModel());
            this.afterChange();
            return undefined;
          } catch (error) {
            return problemDetailToFormErrors(error, documentForm);
          }
        },
      },
    },
  );

  protected loadError(error: unknown): string {
    return loadErrorMessage(error, 'Vehicle not found');
  }

  protected async changeStatus(document: VehicleDocument, status: DocumentStatus): Promise<void> {
    this.statusError.set(null);
    try {
      await firstValueFrom(this.documentApi.changeStatus(this.vehicleId(), document.id, status));
      this.afterChange();
    } catch (error) {
      this.statusError.set(problemDetailMessage(error));
    }
  }

  private afterChange(): void {
    this.statusError.set(null);
    this.documents.reload();
    this.changed.emit();
  }
}
