import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ActivityLog {
  id: number;
  username: string;
  action: string;
  description: string;
  entityType: string;
  entityId: number;
  oldValue: string;
  newValue: string;
  timestamp: string;
  ipAddress: string;
}

export interface SummaryReport {
  tenderId: number;
  tenderName: string;
  bidId: number;
  bidderName: string;
  generatedAt: string;
  totalDocumentsFound: number;
  totalDocumentsMissing: number;
  totalDocumentsDuplicate: number;
  totalDocumentsProcessed: number;
  documentsFound: string[];
  missingDocuments: string[];
  duplicateDocuments: string[];
  isValid: boolean;
  validationMessage: string;
  validationScore: number;
  manualChangesCount: number;
  recentManualChanges: string[];
}

export interface ActivityResponse {
  success: boolean;
  message?: string;
  activities?: ActivityLog[];
  activityId?: number;
  totalPages?: number;
  totalElements?: number;
  currentPage?: number;
  count?: number;
}

export interface ReportResponse {
  success: boolean;
  message?: string;
  report?: SummaryReport;
}

@Injectable({
  providedIn: 'root'
})
export class ActivityReportService {
  private apiUrl = 'http://localhost:8080/api/activity';

  constructor(private http: HttpClient) {}

  /**
   * Log a manual change
   */
  logActivity(username: string, action: string, description: string,
             entityType?: string, entityId?: number,
             oldValue?: string, newValue?: string): Observable<ActivityResponse> {
    let params = new HttpParams()
      .set('username', username)
      .set('action', action)
      .set('description', description);
    
    if (entityType) params = params.set('entityType', entityType);
    if (entityId) params = params.set('entityId', entityId.toString());
    if (oldValue) params = params.set('oldValue', oldValue);
    if (newValue) params = params.set('newValue', newValue);

    return this.http.post<ActivityResponse>(`${this.apiUrl}/log`, null, { params });
  }

  /**
   * Log document validation override
   */
  logDocumentOverride(username: string, bidId: number, documentName: string,
                     previousStatus: boolean, newStatus: boolean): Observable<ActivityResponse> {
    const params = new HttpParams()
      .set('username', username)
      .set('bidId', bidId.toString())
      .set('documentName', documentName)
      .set('previousStatus', previousStatus.toString())
      .set('newStatus', newStatus.toString());

    return this.http.post<ActivityResponse>(`${this.apiUrl}/document-override`, null, { params });
  }

  /**
   * Log manual document acceptance
   */
  logDocumentAcceptance(username: string, bidId: number, documentName: string,
                        reason: string): Observable<ActivityResponse> {
    const params = new HttpParams()
      .set('username', username)
      .set('bidId', bidId.toString())
      .set('documentName', documentName)
      .set('reason', reason);

    return this.http.post<ActivityResponse>(`${this.apiUrl}/document-accept`, null, { params });
  }

  /**
   * Log manual document rejection
   */
  logDocumentRejection(username: string, bidId: number, documentName: string,
                       reason: string): Observable<ActivityResponse> {
    const params = new HttpParams()
      .set('username', username)
      .set('bidId', bidId.toString())
      .set('documentName', documentName)
      .set('reason', reason);

    return this.http.post<ActivityResponse>(`${this.apiUrl}/document-reject`, null, { params });
  }

  /**
   * Get user activity logs
   */
  getUserActivityLogs(username: string, page: number = 0, size: number = 20): Observable<ActivityResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ActivityResponse>(`${this.apiUrl}/user/${username}`, { params });
  }

  /**
   * Get activity logs for a specific entity
   */
  getEntityActivityLogs(entityType: string, entityId: number): Observable<ActivityResponse> {
    return this.http.get<ActivityResponse>(`${this.apiUrl}/entity/${entityType}/${entityId}`);
  }

  /**
   * Get recent activity logs
   */
  getRecentActivities(): Observable<ActivityResponse> {
    return this.http.get<ActivityResponse>(`${this.apiUrl}/recent`);
  }

  /**
   * Generate summary report for a bid
   */
  getBidSummaryReport(bidId: number): Observable<ReportResponse> {
    return this.http.get<ReportResponse>(`${this.apiUrl}/report/bid/${bidId}`);
  }

  /**
   * Generate summary report for a tender
   */
  getTenderSummaryReport(tenderId: number): Observable<ReportResponse> {
    return this.http.get<ReportResponse>(`${this.apiUrl}/report/tender/${tenderId}`);
  }

  /**
   * Log validation summary for a bid - combines found, missing, duplicate in one row
   * Updates existing entry if bid already has validation
   */
  logValidationSummary(username: string, bidId: number, foundCount: number, 
                      missingCount: number, duplicateCount: number,
                      foundDocs: string[] = [], missingDocs: string[] = [], duplicateDocs: string[] = []): Observable<ActivityResponse> {
    const params = new HttpParams()
      .set('username', username)
      .set('bidId', bidId.toString())
      .set('foundCount', foundCount.toString())
      .set('missingCount', missingCount.toString())
      .set('duplicateCount', duplicateCount.toString())
      .set('foundDocs', foundDocs.join(','))
      .set('missingDocs', missingDocs.join(','))
      .set('duplicateDocs', duplicateDocs.join(','));

    return this.http.post<ActivityResponse>(`${this.apiUrl}/validation-summary`, null, { params });
  }
}
