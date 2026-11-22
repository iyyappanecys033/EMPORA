import React, { useState, useEffect, useCallback } from 'react';
import ticketService from '../../services/ticketService';
import { useAuth } from '../../services/AuthContext';
import Alert from '../common/Alert';
import Avatar from '../common/Avatar';
import LoadingSpinner from '../common/LoadingSpinner';
import { LockIcon } from '../common/Icons';

const TicketComments = ({ ticketId }) => {
  const { isResolver, isAdmin } = useAuth();
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [commentText, setCommentText] = useState('');
  const [isInternal, setIsInternal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [alert, setAlert] = useState(null);

  const fetchComments = useCallback(async () => {
    try {
      const data = await ticketService.getComments(ticketId);
      setComments(data || []);
    } catch (err) {
      console.error('Failed to load comments', err);
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  useEffect(() => {
    fetchComments();
  }, [fetchComments]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!commentText.trim()) return;

    setSubmitting(true);
    setAlert(null);
    try {
      await ticketService.addComment(ticketId, commentText.trim(), isInternal);
      setCommentText('');
      setIsInternal(false);
      fetchComments();
    } catch (err) {
      setAlert(err);
    } finally {
      setSubmitting(false);
    }
  };

  const canPostInternal = isResolver || isAdmin;

  return (
    <section style={{ marginTop: '28px' }} aria-labelledby="ticket-discussion-heading">
      <h2 id="ticket-discussion-heading" style={{ fontSize: '16px', fontWeight: 700, color: 'var(--ep-text-primary)', marginBottom: '14px' }}>
        Activity &amp; Discussion ({comments.length})
      </h2>

      {alert && (
        <Alert type="error" title={alert.title} detail={alert.detail} onClose={() => setAlert(null)} />
      )}

      <form onSubmit={handleSubmit} className="ep-card" style={{ marginBottom: '18px' }}>
        <label htmlFor="ticket-comment-input" className="sr-only">Add comment</label>
        <textarea
          id="ticket-comment-input"
          rows={3}
          value={commentText}
          onChange={(e) => setCommentText(e.target.value)}
          placeholder="Add an update, clarification, or note..."
          required
          maxLength={4000}
          disabled={submitting}
          className="ep-textarea"
        />

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px', marginTop: '12px' }}>
          {canPostInternal ? (
            <label style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: 'var(--ep-text-secondary)', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={isInternal}
                onChange={(e) => setIsInternal(e.target.checked)}
              />
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', fontWeight: 600 }}>
                <LockIcon size={14} /> Internal Specialist Note{' '}
                <span style={{ color: 'var(--ep-text-muted)', fontSize: '11px', fontWeight: 500 }}>(Hidden from employee)</span>
              </span>
            </label>
          ) : (
            <div />
          )}

          <button type="submit" disabled={submitting || !commentText.trim()} className="ep-btn ep-btn--primary">
            {submitting ? 'Posting...' : 'Post Comment'}
          </button>
        </div>
      </form>

      {loading ? (
        <LoadingSpinner message="Loading discussion..." />
      ) : comments.length === 0 ? (
        <div
          className="ep-card"
          style={{ textAlign: 'center', padding: '24px', color: 'var(--ep-text-muted)', borderStyle: 'dashed', fontSize: '13px' }}
        >
          No comments yet. Start the conversation above.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {comments.map((c) => (
            <article
              key={c.id}
              className="ep-card"
              style={{
                backgroundColor: c.isInternal ? 'var(--ep-warning-bg)' : 'var(--ep-bg-surface)',
                borderColor: c.isInternal ? 'var(--ep-warning-border)' : 'var(--ep-border)',
                borderLeft: c.isInternal ? '4px solid var(--ep-warning)' : undefined,
                padding: '14px 16px'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px', marginBottom: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Avatar name={c.authorUsername} size={26} />
                  <span style={{ fontWeight: 700, fontSize: '13px', color: 'var(--ep-text-primary)' }}>
                    {c.authorUsername}
                  </span>
                  {c.isInternal && (
                    <span
                      style={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: '4px',
                        fontSize: '10px',
                        padding: '2px 7px',
                        borderRadius: '4px',
                        backgroundColor: 'var(--ep-bg-surface)',
                        color: 'var(--ep-warning-text)',
                        border: '1px solid var(--ep-warning-border)',
                        fontWeight: 700,
                        textTransform: 'uppercase'
                      }}
                    >
                      <LockIcon size={11} /> Internal Note
                    </span>
                  )}
                </div>
                <span className="ep-tabular" style={{ fontSize: '12px', color: 'var(--ep-text-muted)' }}>
                  {c.createdAt ? new Date(c.createdAt).toLocaleString() : ''}
                </span>
              </div>

              <div style={{ fontSize: '13px', color: 'var(--ep-text-primary)', lineHeight: 1.55, whiteSpace: 'pre-wrap' }}>
                {c.commentText}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
};

export default TicketComments;
