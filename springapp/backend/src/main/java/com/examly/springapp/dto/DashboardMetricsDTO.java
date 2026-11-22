package com.examly.springapp.dto;

public class DashboardMetricsDTO {
    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long disabledUsers;
    private long lockedUsers;
    private long highRiskUsers;
    private long criticalRiskUsers;
    private long totalAuditEvents;
    
    // Getters and Setters
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    
    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }
    
    public long getSuspendedUsers() { return suspendedUsers; }
    public void setSuspendedUsers(long suspendedUsers) { this.suspendedUsers = suspendedUsers; }
    
    public long getDisabledUsers() { return disabledUsers; }
    public void setDisabledUsers(long disabledUsers) { this.disabledUsers = disabledUsers; }
    
    public long getLockedUsers() { return lockedUsers; }
    public void setLockedUsers(long lockedUsers) { this.lockedUsers = lockedUsers; }
    
    public long getHighRiskUsers() { return highRiskUsers; }
    public void setHighRiskUsers(long highRiskUsers) { this.highRiskUsers = highRiskUsers; }
    
    public long getCriticalRiskUsers() { return criticalRiskUsers; }
    public void setCriticalRiskUsers(long criticalRiskUsers) { this.criticalRiskUsers = criticalRiskUsers; }
    
    public long getTotalAuditEvents() { return totalAuditEvents; }
    public void setTotalAuditEvents(long totalAuditEvents) { this.totalAuditEvents = totalAuditEvents; }
}
