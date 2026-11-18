import React, { useState, useEffect, useCallback } from 'react';
import ticketService from '../../services/ticketService';
import { useAuth } from '../../services/AuthContext';
import Alert from '../common/Alert';
import LoadingSpinner from '../common/LoadingSpinner';

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
    <div style={{ marginTop: '32px' }}>
      <h3 style={{ fontSize: '16px', fontWeight: 700, color: '#0f172a', marginBottom: '16px' }}>
        Activity & Discussion ({comments.length})
      </h3>

      {alert && (
        <Alert
          type="error"
          title={alert.title}
          detail={alert.detail}
          onClose={() => setAlert(null)}
        />
      )}

      {/* Comment Input Form */}
      <form
        onSubmit={handleSubmit}
        style={{
          backgroundColor: '#fff',
          borderRadius: '8px',
          border: '1px solid #e2e8f0',
          padding: '16px',
          marginBottom: '20px'
        }}
      >
        <textarea
          rows={3}
          value={commentText}
          onChange={(e) => setCommentText(e.target.value)}
          placeholder="Add an update, clarification, or note..."
          required
          maxLength={4000}
          disabled={submitting}
          style={{
            width: '100%',
            padding: '10px 12px',
            borderRadius: '6px',
            border: '1px solid #cbd5e1',
            fontSize: '13px',
            fontFamily: 'inherit',
            resize: 'vertical',
            boxSizing: 'border-box'
          }}
        />

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '10px' }}>
          {canPostInternal ? (
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', color: '#475569', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={isInternal}
                onChange={(e) => setIsInternal(e.target.checked)}
              />
              <span style={{ fontWeight: 500 }}>
                🔒 Internal Specialist Note <span style={{ color: '#94a3b8', fontSize: '11px' }}>(Hidden from employee)</span>
              </span>
            </label>
          ) : (
            <div />
          )}

          <button
            type="submit"
            disabled={submitting || !commentText.trim()}
            style={{
              padding: '8px 18px',
              borderRadius: '6px',
              border: 'none',
              backgroundColor: submitting || !commentText.trim() ? '#93c5fd' : '#2563eb',
              color: '#fff',
              fontSize: '13px',
              fontWeight: 600,
              cursor: submitting || !commentText.trim() ? 'not-allowed' : 'pointer'
            }}
          >
            {submitting ? 'Posting...' : 'Post Comment'}
          </button>
        </div>
      </form>

      {/* Comments List */}
      {loading ? (
        <LoadingSpinner message="Loading discussion..." />
      ) : comments.length === 0 ? (
        <div style={{ padding: '24px', textAlign: 'center', color: '#94a3b8', backgroundColor: '#fff', borderRadius: '8px', border: '1px dashed #cbd5e1', fontSize: '13px' }}>
          No comments yet. Start the conversation above.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {comments.map((c) => (
            <div
              key={c.id}
              style={{
                backgroundColor: c.isInternal ? '#fffbeb' : '#fff',
                borderRadius: '8px',
                border: c.isInternal ? '1px solid #fde68a' : '1px solid #e2e8f0',
                padding: '14px 16px'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ fontWeight: 700, fontSize: '13px', color: '#1e293b' }}>
                    {c.authorUsername}
                  </span>
                  {c.isInternal && (
                    <span
                      style={{
                        fontSize: '10px',
                        padding: '2px 6px',
                        borderRadius: '4px',
                        backgroundColor: '#fef3c7',
                        color: '#92400e',
                        fontWeight: 700,
                        textTransform: 'uppercase'
                      }}
                    >
                      🔒 Internal Note
                    </span>
                  )}
                </div>
                <span style={{ fontSize: '12px', color: '#94a3b8' }}>
                  {c.createdAt ? new Date(c.createdAt).toLocaleString() : ''}
                </span>
              </div>

              <div style={{ fontSize: '13px', color: '#334155', lineHeight: 1.5, whiteSpace: 'pre-wrap' }}>
                {c.commentText}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default TicketComments;

