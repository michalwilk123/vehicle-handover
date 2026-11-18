import { Component, inject, input, numberAttribute } from '@angular/core';
import { loadErrorMessage } from '../shared/problem-detail';
import {
  DOCUMENT_TYPE_LABELS,
  DocumentApi,
  HandoverReadiness as Readiness,
  READINESS_REASON_LABELS,
} from './document-api';

@Component({
  selector: 'app-handover-readiness',
  templateUrl: './handover-readiness.html',
})
export class HandoverReadiness {
  private readonly documentApi = inject(DocumentApi);

  readonly vehicleId = input.required({ transform: numberAttribute });

  protected readonly readiness = this.documentApi.readiness(this.vehicleId);
  protected readonly typeLabels = DOCUMENT_TYPE_LABELS;

  /** Loads the readiness again, for example after a document changes. */
  reload(): void {
    this.readiness.reload();
  }

  protected problemText(problem: Readiness['problems'][number]): string {
    const reason = READINESS_REASON_LABELS[problem.reason];
    return problem.documentType === null
      ? reason
      : `${this.typeLabels[problem.documentType]}: ${reason}`;
  }

  protected loadError(error: unknown): string {
    return loadErrorMessage(error, 'Vehicle not found');
  }
}
