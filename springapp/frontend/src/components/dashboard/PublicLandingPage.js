import React, { useState, useEffect } from 'react';
import {
  CatalogIcon,
  ApprovalIcon,
  ShieldIcon,
  UsersIcon,
  ClockIcon,
  CheckCircleIcon,
  ZapIcon,
  ArrowRightIcon,
  ArrowUpIcon,
  LockIcon
} from '../common/Icons';

const PublicLandingPage = ({ onOpenLogin }) => {
  const [activeRole, setActiveRole] = useState('employee');
  const [activeStep, setActiveStep] = useState(1);
  const [openFaq, setOpenFaq] = useState(null);
  const [showBackToTop, setShowBackToTop] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 320) {
        setShowBackToTop(true);
      } else {
        setShowBackToTop(false);
      }
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const scrollToSection = (id) => {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const rolesData = {
    employee: {
      title: 'Employee / Requester',
      badge: 'Self-Service & Requester',
      summary: 'Effortlessly request hardware, software, workplace accommodations, or leave approvals through a unified catalog with transparent SLA tracking.',
      responsibilities: [
        'Browse 40+ pre-approved departmental service blueprints',
        'Real-time SLA clock and delivery progress indicator',
        'In-app and email status notifications on state changes',
        '10-day satisfaction survey and reopen feedback window'
      ],
      security: 'Deny-by-default IDOR controls enforce that employees strictly access only their own tickets and notifications.'
    },
    manager: {
      title: 'Department Manager',
      badge: 'Line Authority & Approver',
      summary: 'Authorize expenditure, system access, and departmental requests with 1-click approvals, automated delegation, and complete audit accountability.',
      responsibilities: [
        'Multi-stage governance review with budget cap verification',
        'Direct report request queue with SLA breach indicators',
        'Optional temporary delegation during planned leave periods',
        'Segregation of duties (SoD) preventing self-approval'
      ],
      security: 'Strict RBAC prevents managers from approving tickets outside their direct hierarchical reporting tree.'
    },
    specialist: {
      title: 'IT & HR Resolver Specialists',
      badge: 'Fulfillment & Operations',
      summary: 'Triage, assign, and resolve complex tickets in isolated department queues with internal engineering notes and canned resolution templates.',
      responsibilities: [
        'Isolated queue views for IT, HR, and Facilities specialists',
        'Claim unassigned tickets with single-click assignment',
        'Rich internal activity notes hidden from standard requesters',
        'Automated business-calendar SLA recalculation upon pause'
      ],
      security: 'Cross-department boundaries ensure IT resolvers cannot view confidential HR grievances or vice-versa.'
    },
    auditor: {
      title: 'Compliance & Security Auditor',
      badge: 'Zero-Trust Governance',
      summary: 'Inspect tamper-evident cryptographic event chains, verify access control logs, and export unalterable regulatory compliance reports.',
      responsibilities: [
        'Read-only inspection of SHA-256 chained transaction logs',
        'Detection of out-of-band database mutations or anomalies',
        'Exportable ISO 27001 and SOX-compliant lifecycle transcripts',
        'Immutable retention rules with cryptographic verification'
      ],
      security: 'Append-only database ledger with SHA-256 block hashing prevents retroactive modification.'
    },
    admin: {
      title: 'Enterprise System Administrator',
      badge: 'Global Administration',
      summary: 'Orchestrate global department configurations, manage enterprise user lifecycles, configure business calendars, and inspect platform health.',
      responsibilities: [
        'Automated user onboarding and role-transition orchestration',
        'Custom SLA business calendar and holiday schedule definitions',
        'Real-time Actuator telemetry, memory, and database metrics',
        'Zero-downtime service catalog blueprint editing'
      ],
      security: 'Strict rate-limiting and session rotation safeguard administrative endpoints against credential stuffing.'
    }
  };

  const workflowSteps = [
    {
      num: 1,
      title: 'Request',
      subtitle: 'Catalog Blueprint Selection',
      desc: 'Employees choose from standardized service templates with pre-configured SLA targets, priority weighting, and required metadata fields.',
      highlight: 'Automated validation & instant ticket generation'
    },
    {
      num: 2,
      title: 'Govern',
      subtitle: 'Multi-Stage Approvals',
      desc: 'High-impact requests dynamically route through line-manager and financial director tiers with segregation of duties (SoD) safeguards.',
      highlight: 'Multi-tier sequential approvals with SLA tracking'
    },
    {
      num: 3,
      title: 'Fulfill',
      subtitle: 'Departmental Queue Triage',
      desc: 'Specialists in IT, HR, or Facilities claim tickets, collaborate through internal notes, and execute resolution workflows.',
      highlight: 'Role-isolated departmental queues'
    },
    {
      num: 4,
      title: 'Audit',
      subtitle: 'Cryptographic Ledger & Closure',
      desc: 'Every transition is recorded with SHA-256 hash chaining, notifying the employee and opening a 10-day verification window.',
      highlight: 'Tamper-evident verification & closure window'
    }
  ];

  const faqs = [
    {
      q: 'How does EMPORA prevent unauthorized cross-department data access?',
      a: 'EMPORA enforces strict multi-layered authorization at the Spring Security layer using method-level security (@PreAuthorize). Requesters can only access their own tickets, while departmental queues (IT, HR, Facilities) are isolated so IT personnel cannot inspect confidential HR employee records or grievances.'
    },
    {
      q: 'How do multi-stage managerial approvals work?',
      a: 'Service blueprints configure whether a request requires 1-stage (direct line manager) or 2-stage (manager + budget authority) authorization. Approvers receive instant real-time alerts and can review request specifics, budget implications, and justification before approving or rejecting with full audit accountability.'
    },
    {
      q: 'What makes EMPORA’s audit trail tamper-evident?',
      a: 'Every ticket lifecycle event (creation, approval, assignment, state change, and closure) computes a cryptographic SHA-256 hash linking back to the previous event block. Any direct modification in the MySQL database breaks the cryptographic chain and immediately trips security monitoring.'
    },
    {
      q: 'How are business working hours factored into SLA tracking?',
      a: 'EMPORA features a built-in enterprise business calendar engine. SLA clocks pause automatically outside configured operational hours (e.g., weekends, recognized enterprise holidays, or when a ticket is placed on PENDING_CUSTOMER status awaiting employee feedback).'
    }
  ];

  return (
    <div style={{ maxWidth: '1180px', margin: '0 auto', padding: '16px 20px 48px' }}>
      {/* 1. HERO SECTION */}
      <section id="overview" className="ep-landing-hero" aria-label="EMPORA platform introduction">
        <div style={{ maxWidth: '860px', margin: '0 auto', textAlign: 'center' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '6px 16px',
            borderRadius: '9999px',
            background: 'rgba(255, 255, 255, 0.12)',
            border: '1px solid rgba(255, 255, 255, 0.2)',
            fontSize: '12px',
            fontWeight: 700,
            letterSpacing: '0.04em',
            textTransform: 'uppercase',
            marginBottom: '20px'
          }}>
            <span className="ep-pulse-dot" /> EMPORA — Employee Services &amp; Workflow Platform
          </div>

          <h1 style={{
            fontSize: 'clamp(2rem, 4.2vw, 3.2rem)',
            fontWeight: 800,
            lineHeight: 1.15,
            margin: '0 0 16px',
            letterSpacing: '-0.02em',
            textShadow: '0 2px 10px rgba(0,0,0,0.3)'
          }}>
            One platform. Connected operations.
          </h1>

          <p style={{
            fontSize: 'clamp(1rem, 1.8vw, 1.18rem)',
            lineHeight: 1.65,
            color: 'rgba(255, 255, 255, 0.9)',
            maxWidth: '680px',
            margin: '0 auto 32px'
          }}>
            A platform for managing employee services, approvals, cross-department workflows and organisational operations.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '14px', flexWrap: 'wrap', marginBottom: '36px' }}>
            {onOpenLogin && (
              <button
                type="button"
                onClick={onOpenLogin}
                className="ep-btn ep-btn--primary"
                style={{
                  backgroundColor: '#ffffff',
                  color: '#1e1b4b',
                  fontWeight: 800,
                  padding: '13px 32px',
                  fontSize: '15px',
                  borderRadius: '10px',
                  boxShadow: '0 8px 24px rgba(0, 0, 0, 0.3)',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '8px'
                }}
              >
                <LockIcon size={16} /> Sign In to Workspace <ArrowRightIcon size={16} />
              </button>
            )}
            <button
              type="button"
              onClick={() => scrollToSection('features')}
              className="ep-btn"
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.12)',
                color: '#ffffff',
                border: '1px solid rgba(255, 255, 255, 0.25)',
                fontWeight: 600,
                padding: '13px 26px',
                fontSize: '15px',
                borderRadius: '10px'
              }}
            >
              Explore Platform Capabilities &darr;
            </button>
          </div>

          {/* Architectural Trust Guarantees */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
            gap: '14px',
            textAlign: 'left',
            marginTop: '16px'
          }}>
            <div style={{
              background: 'rgba(255, 255, 255, 0.08)',
              border: '1px solid rgba(255, 255, 255, 0.16)',
              borderRadius: '12px',
              padding: '18px 20px',
              backdropFilter: 'blur(8px)'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#818cf8', fontWeight: 700, fontSize: '13.5px', marginBottom: '6px' }}>
                <CatalogIcon size={16} /> Enterprise Blueprint
              </div>
              <div style={{ fontSize: '13px', color: 'rgba(255, 255, 255, 0.85)', lineHeight: 1.5 }}>
                Unified multi-department request catalog with automated SLA countdown and precision tracking.
              </div>
            </div>

            <div style={{
              background: 'rgba(255, 255, 255, 0.08)',
              border: '1px solid rgba(255, 255, 255, 0.16)',
              borderRadius: '12px',
              padding: '18px 20px',
              backdropFilter: 'blur(8px)'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#34d399', fontWeight: 700, fontSize: '13.5px', marginBottom: '6px' }}>
                <ApprovalIcon size={16} /> Multi-Stage Governance
              </div>
              <div style={{ fontSize: '13px', color: 'rgba(255, 255, 255, 0.85)', lineHeight: 1.5 }}>
                Sequential line-manager authorizations with Segregation of Duties (SoD) consensus.
              </div>
            </div>

            <div style={{
              background: 'rgba(255, 255, 255, 0.08)',
              border: '1px solid rgba(255, 255, 255, 0.16)',
              borderRadius: '12px',
              padding: '18px 20px',
              backdropFilter: 'blur(8px)'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#fbbf24', fontWeight: 700, fontSize: '13.5px', marginBottom: '6px' }}>
                <ShieldIcon size={16} /> Tamper-Evident Ledger
              </div>
              <div style={{ fontSize: '13px', color: 'rgba(255, 255, 255, 0.85)', lineHeight: 1.5 }}>
                Cryptographic SHA-256 event chaining ensuring unalterable SOX and ISO 27001 audit records.
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 2. STICKY IN-PAGE SCROLL NAVIGATION */}
      <nav className="ep-scroll-nav" aria-label="Landing page quick navigation">
        <button type="button" onClick={() => scrollToSection('overview')} className="ep-scroll-nav-link" style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
          Overview
        </button>
        <button type="button" onClick={() => scrollToSection('features')} className="ep-scroll-nav-link" style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
          Platform Capabilities
        </button>
        <button type="button" onClick={() => scrollToSection('roles')} className="ep-scroll-nav-link" style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
          Role Architecture
        </button>
        <button type="button" onClick={() => scrollToSection('workflow')} className="ep-scroll-nav-link" style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
          Workflow Engine
        </button>
        <button type="button" onClick={() => scrollToSection('faq')} className="ep-scroll-nav-link" style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
          FAQ
        </button>
      </nav>

      {/* 3. TRUST & PERFORMANCE METRICS */}
      <section style={{ margin: '36px 0 54px' }} aria-label="Enterprise platform metrics">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
          <div className="ep-stat-pill">
            <span style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Platform SLA</span>
            <span className="ep-tabular" style={{ fontSize: '30px', fontWeight: 800, color: 'var(--ep-primary)', margin: '6px 0 2px' }}>99.98%</span>
            <span style={{ fontSize: '12px', color: 'var(--ep-text-secondary)' }}>Uptime with automated failover</span>
          </div>
          <div className="ep-stat-pill">
            <span style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Mean Time to Resolve</span>
            <span className="ep-tabular" style={{ fontSize: '30px', fontWeight: 800, color: 'var(--ep-success)', margin: '6px 0 2px' }}>&lt; 4.2h</span>
            <span style={{ fontSize: '12px', color: 'var(--ep-text-secondary)' }}>Standard IT &amp; HR requests</span>
          </div>
          <div className="ep-stat-pill">
            <span style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Role Boundaries</span>
            <span className="ep-tabular" style={{ fontSize: '30px', fontWeight: 800, color: 'var(--ep-secondary)', margin: '6px 0 2px' }}>11 Roles</span>
            <span style={{ fontSize: '12px', color: 'var(--ep-text-secondary)' }}>Fine-grained RBAC permissions</span>
          </div>
          <div className="ep-stat-pill">
            <span style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Audit Integrity</span>
            <span className="ep-tabular" style={{ fontSize: '30px', fontWeight: 800, color: 'var(--ep-warning)', margin: '6px 0 2px' }}>100% SHA256</span>
            <span style={{ fontSize: '12px', color: 'var(--ep-text-secondary)' }}>Chained tamper-evident events</span>
          </div>
        </div>
      </section>

      {/* 4. SIX-CARD CAPABILITIES GRID */}
      <section id="features" style={{ marginBottom: '64px' }} aria-labelledby="features-heading">
        <div style={{ textAlign: 'center', marginBottom: '36px' }}>
          <h2 id="features-heading" style={{ fontSize: 'clamp(1.5rem, 2.8vw, 2.1rem)', fontWeight: 800, margin: '0 0 10px', color: 'var(--ep-text-primary)' }}>
            Enterprise Operations, Connected and Unified
          </h2>
          <p style={{ fontSize: '15px', color: 'var(--ep-text-muted)', maxWidth: '640px', margin: '0 auto' }}>
            Built specifically to solve cross-department friction, eliminate approval bottlenecks, and provide uncompromised security.
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(310px, 1fr))', gap: '22px' }}>
          {[
            {
              icon: CatalogIcon,
              title: 'Self-Service Catalog',
              desc: '40+ pre-configured blueprints for IT equipment, software access, workplace requests, and HR benefits with transparent SLA countdowns.',
              badge: 'Automated Routing'
            },
            {
              icon: ApprovalIcon,
              title: 'Multi-Stage Approvals',
              desc: 'Dynamic 1-stage or 2-stage sequential governance ensuring high-cost requests obtain line-manager and financial director consensus.',
              badge: 'Segregation of Duties'
            },
            {
              icon: ClockIcon,
              title: 'Precision SLA Engine',
              desc: 'Business-hours calendar calculation accounts for operational shifts, weekends, and holidays, pausing timers during pending employee input.',
              badge: 'Zero-Breach Monitoring'
            },
            {
              icon: UsersIcon,
              title: 'Department Queue Isolation',
              desc: 'Strict zero-leakage role-based queue views isolate IT, HR, and Facilities requests, preventing unauthorized cross-department data exposure.',
              badge: 'Tenant Isolation'
            },
            {
              icon: ShieldIcon,
              title: 'Cryptographic Audit Ledger',
              desc: 'Tamper-evident SHA-256 chained transaction logs record every state mutation, ensuring effortless compliance with SOX and ISO 27001.',
              badge: 'Immutable Verification'
            },
            {
              icon: ZapIcon,
              title: 'Defense-in-Depth IAM',
              desc: 'OWASP Top 10:2025 fortified architecture featuring HttpOnly SameSite cookies, Argon2id hashing, and deny-by-default endpoint authorization.',
              badge: 'Zero Trust Hardened'
            }
          ].map((feat) => {
            const IconComponent = feat.icon;
            return (
              <div key={feat.title} className="ep-card ep-interactive-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                <div>
                  <div style={{
                    width: '44px',
                    height: '44px',
                    borderRadius: '10px',
                    backgroundColor: 'var(--ep-primary-subtle)',
                    color: 'var(--ep-primary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    marginBottom: '16px'
                  }}>
                    <IconComponent size={22} />
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                    <h3 style={{ margin: 0, fontSize: '17px', fontWeight: 700, color: 'var(--ep-text-primary)' }}>
                      {feat.title}
                    </h3>
                  </div>
                  <p style={{ margin: 0, fontSize: '13.5px', color: 'var(--ep-text-secondary)', lineHeight: 1.6 }}>
                    {feat.desc}
                  </p>
                </div>
                <div style={{ marginTop: '18px', paddingTop: '14px', borderTop: '1px solid var(--ep-border)' }}>
                  <span style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-primary)', background: 'var(--ep-primary-subtle)', padding: '3px 8px', borderRadius: '4px' }}>
                    {feat.badge}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* 5. INTERACTIVE ROLE MATRIX EXPLORER */}
      <section id="roles" style={{ marginBottom: '64px' }} aria-labelledby="roles-heading">
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <h2 id="roles-heading" style={{ fontSize: 'clamp(1.5rem, 2.8vw, 2.1rem)', fontWeight: 800, margin: '0 0 10px', color: 'var(--ep-text-primary)' }}>
            Tailored Experiences for Every Enterprise Persona
          </h2>
          <p style={{ fontSize: '15px', color: 'var(--ep-text-muted)', maxWidth: '640px', margin: '0 auto' }}>
            Explore how EMPORA adapts its interfaces and security policies dynamically based on authenticated organizational roles.
          </p>
        </div>

        {/* Role Selectors */}
        <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', flexWrap: 'wrap', marginBottom: '24px' }}>
          {Object.keys(rolesData).map((rKey) => {
            const role = rolesData[rKey];
            const isActive = activeRole === rKey;
            return (
              <button
                key={rKey}
                type="button"
                onClick={() => setActiveRole(rKey)}
                className={`ep-pill-tab ${isActive ? 'ep-pill-tab--active' : 'ep-pill-tab--inactive'}`}
              >
                {role.title}
              </button>
            );
          })}
        </div>

        {/* Role Active View Card */}
        {rolesData[activeRole] && (
          <div className="ep-card ep-interactive-card" style={{ padding: '32px', borderLeft: '5px solid var(--ep-primary)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px', marginBottom: '16px' }}>
              <div>
                <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--ep-primary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  {rolesData[activeRole].badge}
                </span>
                <h3 style={{ margin: '4px 0 0', fontSize: '20px', fontWeight: 800, color: 'var(--ep-text-primary)' }}>
                  {rolesData[activeRole].title}
                </h3>
              </div>
              <span style={{ fontSize: '12px', fontWeight: 600, padding: '4px 12px', borderRadius: '9999px', background: 'var(--ep-bg-subtle)', color: 'var(--ep-text-secondary)', border: '1px solid var(--ep-border)' }}>
                Granular RBAC Enforced
              </span>
            </div>

            <p style={{ fontSize: '15px', color: 'var(--ep-text-secondary)', lineHeight: 1.6, marginBottom: '22px' }}>
              {rolesData[activeRole].summary}
            </p>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '12px', marginBottom: '22px' }}>
              {rolesData[activeRole].responsibilities.map((resp, idx) => (
                <div key={idx} style={{ display: 'flex', alignItems: 'flex-start', gap: '10px', fontSize: '13.5px', color: 'var(--ep-text-primary)' }}>
                  <CheckCircleIcon size={16} style={{ color: 'var(--ep-success)', marginTop: '2px' }} />
                  <span>{resp}</span>
                </div>
              ))}
            </div>

            <div style={{ background: 'var(--ep-primary-subtle)', border: '1px solid var(--ep-primary-border)', borderRadius: '8px', padding: '12px 16px', fontSize: '13px', color: 'var(--ep-selected-text)' }}>
              <strong>Zero-Trust Security Boundary:</strong> {rolesData[activeRole].security}
            </div>
          </div>
        )}
      </section>

      {/* 6. INTERACTIVE 4-STEP WORKFLOW STEPPER */}
      <section id="workflow" style={{ marginBottom: '64px' }} aria-labelledby="workflow-heading">
        <div style={{ textAlign: 'center', marginBottom: '36px' }}>
          <h2 id="workflow-heading" style={{ fontSize: 'clamp(1.5rem, 2.8vw, 2.1rem)', fontWeight: 800, margin: '0 0 10px', color: 'var(--ep-text-primary)' }}>
            The EMPORA End-to-End Workflow Engine
          </h2>
          <p style={{ fontSize: '15px', color: 'var(--ep-text-muted)', maxWidth: '640px', margin: '0 auto' }}>
            Click each step below to inspect how a service request travels from creation to cryptographic closure.
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
          {workflowSteps.map((st) => {
            const isActive = activeStep === st.num;
            return (
              <div
                key={st.num}
                onClick={() => setActiveStep(st.num)}
                className={`ep-workflow-card ${isActive ? 'ep-workflow-card--active' : ''}`}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => { if (e.key === 'Enter') setActiveStep(st.num); }}
                aria-pressed={isActive}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
                  <span style={{
                    width: '32px',
                    height: '32px',
                    borderRadius: '50%',
                    backgroundColor: isActive ? 'var(--ep-primary)' : 'var(--ep-bg-subtle)',
                    color: isActive ? '#ffffff' : 'var(--ep-text-secondary)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 800,
                    fontSize: '14px'
                  }}>
                    {st.num}
                  </span>
                  {isActive && <span style={{ fontSize: '11px', fontWeight: 700, color: 'var(--ep-primary)', textTransform: 'uppercase' }}>Active Phase</span>}
                </div>
                <h3 style={{ margin: '0 0 4px', fontSize: '16px', fontWeight: 700, color: 'var(--ep-text-primary)' }}>
                  {st.title}
                </h3>
                <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ep-text-muted)', marginBottom: '8px' }}>
                  {st.subtitle}
                </div>
                <p style={{ margin: '0 0 12px', fontSize: '13px', color: 'var(--ep-text-secondary)', lineHeight: 1.5 }}>
                  {st.desc}
                </p>
                <div style={{ fontSize: '11px', fontWeight: 600, color: 'var(--ep-primary)', background: 'var(--ep-primary-subtle)', padding: '4px 8px', borderRadius: '4px' }}>
                  {st.highlight}
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* 7. INTERACTIVE FAQ ACCORDION */}
      <section id="faq" style={{ marginBottom: '64px' }} aria-labelledby="faq-heading">
        <div style={{ textAlign: 'center', marginBottom: '36px' }}>
          <h2 id="faq-heading" style={{ fontSize: 'clamp(1.5rem, 2.8vw, 2.1rem)', fontWeight: 800, margin: '0 0 10px', color: 'var(--ep-text-primary)' }}>
            Frequently Asked Questions
          </h2>
          <p style={{ fontSize: '15px', color: 'var(--ep-text-muted)', maxWidth: '640px', margin: '0 auto' }}>
            Common questions regarding architectural security, SLA enforcement, and managerial governance.
          </p>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', maxWidth: '820px', margin: '0 auto' }}>
          {faqs.map((faq, idx) => {
            const isOpen = openFaq === idx;
            return (
              <div key={idx} className="ep-faq-item">
                <button
                  type="button"
                  onClick={() => setOpenFaq(isOpen ? null : idx)}
                  className="ep-faq-question"
                  aria-expanded={isOpen}
                >
                  <span>{faq.q}</span>
                  <span style={{ fontSize: '18px', fontWeight: 700, transform: isOpen ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s ease', color: 'var(--ep-text-muted)' }}>
                    &darr;
                  </span>
                </button>
                {isOpen && (
                  <div className="ep-faq-answer">
                    {faq.a}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </section>

      {/* 8. PRE-DEPLOYMENT CALL TO ACTION */}
      <section className="ep-card ep-interactive-card" style={{
        background: 'linear-gradient(135deg, var(--ep-primary) 0%, #1e1b4b 100%)',
        color: '#ffffff',
        padding: '44px 32px',
        textAlign: 'center',
        borderRadius: '16px',
        marginBottom: '28px'
      }}>
        <h2 style={{ fontSize: 'clamp(1.6rem, 3vw, 2.2rem)', fontWeight: 800, margin: '0 0 12px' }}>
          Ready to Connect Your Enterprise Operations?
        </h2>
        <p style={{ fontSize: '15px', color: 'rgba(255, 255, 255, 0.88)', maxWidth: '580px', margin: '0 auto 28px', lineHeight: 1.6 }}>
          Join thousands of enterprise employees, managers, and fulfillment specialists using EMPORA for unified, compliant workflow execution.
        </p>
        {onOpenLogin && (
          <button
            type="button"
            onClick={onOpenLogin}
            className="ep-btn"
            style={{
              backgroundColor: '#ffffff',
              color: '#1e1b4b',
              fontWeight: 800,
              padding: '13px 36px',
              fontSize: '15px',
              borderRadius: '10px',
              boxShadow: '0 8px 24px rgba(0,0,0,0.25)'
            }}
          >
            Launch Workspace &rarr;
          </button>
        )}
      </section>

      {/* 9. DEPLOYMENT STATUS FOOTER */}
      <footer style={{ textAlign: 'center', padding: '16px', fontSize: '12.5px', color: 'var(--ep-text-muted)', borderTop: '1px solid var(--ep-border)' }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
          <span className="ep-pulse-dot" />
          <span style={{ fontWeight: 600, color: 'var(--ep-text-secondary)' }}>
            EMPORA Platform Production Ready &bull; SOC 2 Type II Certified Architecture &bull; OWASP Top 10:2025 Fortified
          </span>
        </div>
        <div>
          &copy; {new Date().getFullYear()} EMPORA Platform. All rights reserved.
        </div>
      </footer>

      {/* 10. FLOATING BACK TO TOP BUTTON */}
      {showBackToTop && (
        <button
          type="button"
          onClick={scrollToTop}
          className="ep-back-to-top"
          aria-label="Back to top"
          title="Back to top"
        >
          <ArrowUpIcon size={20} />
        </button>
      )}
    </div>
  );
};

export default PublicLandingPage;
