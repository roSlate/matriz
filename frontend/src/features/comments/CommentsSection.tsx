import { useEffect, useRef, useState, type FormEvent } from "react";
import { ApiError, errorMessage } from "../../api/client";
import { addComment, listComments, type Comment } from "../../api/comments";
import { Alert } from "../../components/ui/Alert";
import { Button } from "../../components/ui/Button";
import { TextArea } from "../../components/ui/TextArea";

const CONTENT_MAX = 2000;

type Loaded = { comments: Comment[]; nextPage: number; totalPages: number; totalItems: number };
type LoadState = { status: "loading" } | { status: "error" } | { status: "ready"; data: Loaded };

const formatDateTime = (iso: string) =>
  new Date(iso).toLocaleString("en-GB", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });

// Characters are counted like the server counts them: after trimming, by code point.
const length = (text: string) => [...text.trim()].length;

/** Adds the comments of a page, skipping any that are already shown. */
function append(data: Loaded, items: Comment[], totalPages: number, totalItems: number): Loaded {
  const known = new Set(data.comments.map((comment) => comment.id));
  return {
    comments: [...data.comments, ...items.filter((comment) => !known.has(comment.id))],
    nextPage: data.nextPage + 1,
    totalPages,
    totalItems,
  };
}

type CommentsSectionProps = {
  taskId: number;
  /** Owners and Members may write comments; Viewers only read them. */
  canWrite: boolean;
};

/**
 * The comments of a task, oldest first, with "Show more" when there are more than one page, and a
 * form for Owners and Members. The text shown is always what the server confirmed.
 */
export function CommentsSection({ taskId, canWrite }: CommentsSectionProps) {
  const [state, setState] = useState<LoadState>({ status: "loading" });
  const [reloadKey, setReloadKey] = useState(0);
  const [loadingMore, setLoadingMore] = useState(false);
  const [moreError, setMoreError] = useState(false);
  const [text, setText] = useState("");
  const [fieldError, setFieldError] = useState<string | undefined>();
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const inFlight = useRef(false); // blocks a second submission before the first has re-rendered

  useEffect(() => {
    let cancelled = false;
    setState({ status: "loading" });
    listComments(taskId, 0)
      .then((page) => {
        if (!cancelled) {
          setState({
            status: "ready",
            data: { comments: page.items, nextPage: 1, totalPages: page.totalPages, totalItems: page.totalItems },
          });
        }
      })
      .catch(() => !cancelled && setState({ status: "error" }));
    return () => {
      cancelled = true;
    };
  }, [taskId, reloadKey]);

  async function showMore() {
    if (state.status !== "ready" || loadingMore) {
      return;
    }
    setLoadingMore(true);
    setMoreError(false);
    try {
      const page = await listComments(taskId, state.data.nextPage);
      setState({ status: "ready", data: append(state.data, page.items, page.totalPages, page.totalItems) });
    } catch {
      setMoreError(true);
    } finally {
      setLoadingMore(false);
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current || state.status !== "ready") {
      return;
    }
    const size = length(text);
    setFormError(null);
    if (size === 0) {
      setFieldError("Must not be empty.");
      return;
    }
    if (size > CONTENT_MAX) {
      setFieldError(`Must be at most ${CONTENT_MAX} characters.`);
      return;
    }
    setFieldError(undefined);

    inFlight.current = true;
    setSubmitting(true);
    try {
      const created = await addComment(taskId, text.trim());
      let data = state.data;
      const everythingLoaded = data.comments.length >= data.totalItems;
      if (everythingLoaded) {
        // The new comment is the newest, so it goes at the end.
        data = { ...data, comments: [...data.comments, created], totalItems: data.totalItems + 1 };
      } else {
        // Some older pages are not shown yet: load the rest so the person sees their own comment.
        data = { ...data, totalItems: data.totalItems + 1 };
        while (data.nextPage < data.totalPages) {
          const page = await listComments(taskId, data.nextPage);
          data = append(data, page.items, page.totalPages, page.totalItems);
        }
      }
      setState({ status: "ready", data });
      setText("");
    } catch (error) {
      if (error instanceof ApiError && error.fieldErrors.some((item) => item.field === "content")) {
        setFieldError(error.fieldErrors.find((item) => item.field === "content")?.message);
      } else {
        setFormError(errorMessage(error));
      }
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  const data = state.status === "ready" ? state.data : null;
  const remaining = data ? data.totalItems - data.comments.length : 0;

  return (
    <section className="comments" aria-labelledby="comments-heading">
      <h2 id="comments-heading" className="section-title">
        Comments
      </h2>

      {state.status === "loading" && <p role="status">Loading comments…</p>}

      {state.status === "error" && (
        <Alert tone="error">
          <p className="alert__text">Could not load the comments.</p>
          <Button variant="secondary" onClick={() => setReloadKey((key) => key + 1)}>
            Try again
          </Button>
        </Alert>
      )}

      {data && data.comments.length === 0 && <p className="muted">No comments yet.</p>}

      {data && data.comments.length > 0 && (
        <ul className="comment-list" aria-label="Comments">
          {data.comments.map((comment) => (
            <li key={comment.id} className="comment">
              <p className="comment__meta">
                <strong>{comment.author.name}</strong>
                <time dateTime={comment.createdAt}>{formatDateTime(comment.createdAt)}</time>
              </p>
              <p className="comment__text">{comment.content}</p>
            </li>
          ))}
        </ul>
      )}

      {data && remaining > 0 && (
        <div className="pager">
          <span>
            Showing {data.comments.length} of {data.totalItems} comments
          </span>
          <Button variant="secondary" disabled={loadingMore} onClick={() => void showMore()}>
            {loadingMore ? "Loading…" : "Show more"}
          </Button>
        </div>
      )}
      {moreError && <Alert tone="error">Could not load more comments. Please try again.</Alert>}

      {data && canWrite && (
        <form onSubmit={handleSubmit} noValidate className="comment-form">
          {formError && <Alert tone="error">{formError}</Alert>}
          <TextArea
            label="Add a comment"
            rows={3}
            value={text}
            onChange={(event) => setText(event.target.value)}
            error={fieldError}
          />
          <Button type="submit" disabled={submitting}>
            {submitting ? "Posting…" : "Comment"}
          </Button>
        </form>
      )}

      {data && !canWrite && <p className="muted">Viewers can read comments but not write them.</p>}
    </section>
  );
}
