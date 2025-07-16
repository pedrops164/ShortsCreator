'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { MoreVertical, Send, FilePen, ChevronLeft, ChevronRight } from 'lucide-react';

// --- Types and Mock Data ---

// Define the Draft type with 'Complete' or 'Incomplete' status
interface Draft {
  id: string;
  title: string;
  template: string;
  date: string;
  status: 'Complete' | 'Incomplete';
}

// Create mock data for drafts
const initialDrafts: Draft[] = [
  { id: 'draft-001', title: 'How to build a PC in 2025', template: 'Tutorial Video', date: '2025-07-16T01:15:00Z', status: 'Complete' },
  { id: 'draft-002', title: 'My honest review of the new Tesla phone', template: 'Product Review', date: '2025-07-15T18:30:00Z', status: 'Incomplete' },
  { id: 'draft-003', title: 'Top 5 places to visit in Portugal', template: 'Travel Vlog', date: '2025-07-15T11:00:00Z', status: 'Complete' },
  { id: 'draft-004', title: 'A day in the life of a software engineer', template: 'Day in the Life', date: '2025-07-14T09:00:00Z', status: 'Complete' },
  { id: 'draft-005', title: 'Cooking the perfect steak', template: 'Cooking Show', date: '2025-07-13T16:45:00Z', status: 'Incomplete' },
  { id: 'draft-006', title: 'Unboxing the latest Apple Vision Pro 2', template: 'Product Review', date: '2025-07-12T13:20:00Z', status: 'Complete' },
];

const formatDate = (isoString: string): string => {
  const date = new Date(isoString);
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
};

// --- Reusable Sub-components ---

// Update StatusBadge for Drafts
const StatusBadge = ({ status }: { status: Draft['status'] }) => {
  const statusStyles = {
    Complete: {
      bgColor: 'bg-green-500/10',
      textColor: 'text-green-400',
      icon: <FilePen className="w-4 h-4" />,
      text: 'Complete',
    },
    Incomplete: {
      bgColor: 'bg-yellow-500/10',
      textColor: 'text-yellow-400',
      icon: <FilePen className="w-4 h-4" />,
      text: 'Incomplete',
    },
  };

  const style = statusStyles[status];
  return (
    <div className={`inline-flex items-center gap-x-2 px-3 py-1 rounded-full text-sm font-medium ${style.bgColor} ${style.textColor}`}>
      {style.icon}
      <span>{style.text}</span>
    </div>
  );
};

// Create DraftCard for mobile, which navigates on click
const DraftCard = ({ draft }: { draft: Draft }) => {
  const router = useRouter();
  const handleSubmitClick = (e: React.MouseEvent) => {
    e.stopPropagation(); // Prevent navigation when clicking the button
    alert(`Submitting draft: ${draft.title}`);
  };

  return (
    <div
      onClick={() => router.push(`/drafts/edit/${draft.id}`)}
      className="bg-background/50 rounded-lg p-4 mb-4 border border-accent flex flex-col space-y-3 cursor-pointer hover:border-primary transition-colors"
    >
      <div className="flex justify-between items-start">
        <h3 className="text-foreground font-semibold text-base flex-1 pr-2">{draft.title}</h3>
        <button onClick={(e) => e.stopPropagation()} className="text-accent hover:text-foreground">
          <MoreVertical size={20} />
        </button>
      </div>
      <div className="text-accent text-sm">
        <p><span className="font-medium text-foreground/80">Template:</span> {draft.template}</p>
        <p><span className="font-medium text-foreground/80">Created:</span> {formatDate(draft.date)}</p>
      </div>
      <div className="flex justify-between items-center pt-2">
        <StatusBadge status={draft.status} />
        <button
          onClick={handleSubmitClick}
          disabled={draft.status !== 'Complete'}
          className="flex items-center gap-2 bg-primary text-white font-bold py-2 px-4 rounded-md hover:bg-primary/90 transition-colors disabled:bg-accent/30 disabled:text-foreground/50 disabled:cursor-not-allowed"
        >
          <Send size={16} />
          <span>Submit</span>
        </button>
      </div>
    </div>
  );
};


// --- Main Drafts Page Component ---
export default function DraftsPage() {
  const router = useRouter();
  const [drafts] = useState(initialDrafts);
  const [currentPage, setCurrentPage] = useState(1);
  const draftsPerPage = 5;

  // Pagination logic
  const indexOfLastDraft = currentPage * draftsPerPage;
  const indexOfFirstDraft = indexOfLastDraft - draftsPerPage;
  const currentDrafts = drafts.slice(indexOfFirstDraft, indexOfLastDraft);
  const totalPages = Math.ceil(drafts.length / draftsPerPage);

  const paginate = (pageNumber: number) => setCurrentPage(pageNumber);

  const handleSubmitClick = (e: React.MouseEvent, title: string) => {
    e.stopPropagation(); // Prevent the row's onClick from firing
    alert(`Submitting draft: ${title}`);
  };

  return (
    <main className="flex-1 p-4 sm:p-6 lg:p-8">
      <div className="max-w-7xl mx-auto">
        <header className="mb-8">
          <h2 className="text-3xl font-bold text-foreground tracking-tight">Drafts</h2>
          <p className="text-accent mt-1">
            Manage your content drafts here. Click a draft to continue editing.
          </p>
        </header>

        {/* --- Drafts List Container --- */}
        <div className="bg-background/50 border border-accent/50 rounded-xl shadow-lg">
          {/* Desktop Table */}
          <div className="hidden md:block">
            <table className="w-full text-left">
              <thead className="border-b border-accent/50">
                <tr>
                  <th className="p-4 text-sm font-semibold text-foreground/80">Title</th>
                  <th className="p-4 text-sm font-semibold text-foreground/80">Template</th>
                  <th className="p-4 text-sm font-semibold text-foreground/80">Created</th>
                  <th className="p-4 text-sm font-semibold text-foreground/80">Status</th>
                  <th className="p-4 text-sm font-semibold text-foreground/80 text-center">Actions</th>
                </tr>
              </thead>
              <tbody>
                {currentDrafts.map((draft) => (
                  <tr
                    key={draft.id}
                    onClick={() => router.push(`/drafts/edit/${draft.id}`)}
                    className="border-b border-accent/50 hover:bg-accent/10 transition-colors cursor-pointer"
                  >
                    <td className="p-4 text-foreground font-medium max-w-xs truncate">{draft.title}</td>
                    <td className="p-4 text-accent">{draft.template}</td>
                    <td className="p-4 text-accent">{formatDate(draft.date)}</td>
                    <td className="p-4"><StatusBadge status={draft.status} /></td>
                    <td className="p-4 text-center">
                      <button
                        onClick={(e) => handleSubmitClick(e, draft.title)}
                        disabled={draft.status !== 'Complete'}
                        className="flex items-center justify-center mx-auto gap-2 bg-primary text-white font-bold py-2 px-3 rounded-md hover:bg-primary/90 transition-colors disabled:bg-accent/30 disabled:text-foreground/50 disabled:cursor-not-allowed"
                      >
                        <Send size={16} />
                        <span>Submit</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Cards */}
          <div className="md:hidden p-4">
            {currentDrafts.map(draft => <DraftCard key={draft.id} draft={draft} />)}
          </div>

          {/* --- Pagination --- */}
          <div className="flex items-center justify-between p-4">
            <button
              onClick={() => paginate(currentPage - 1)}
              disabled={currentPage === 1}
              className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-foreground bg-accent/20 rounded-md hover:bg-accent/30 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ChevronLeft size={16} /> Previous
            </button>
            <span className="text-sm text-accent">
              Page {currentPage} of {totalPages}
            </span>
            <button
              onClick={() => paginate(currentPage + 1)}
              disabled={currentPage === totalPages}
              className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-foreground bg-accent/20 rounded-md hover:bg-accent/30 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}