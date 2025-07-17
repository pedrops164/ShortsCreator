'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { MoreVertical, ChevronLeft, ChevronRight } from 'lucide-react';
import apiClient from '@/lib/apiClient';
import { Draft } from '@/types/drafts';
import { getDraftTitle } from '@/lib/helper';

const formatDate = (isoString: string): string => {
  const date = new Date(isoString);
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
};


// Create DraftCard for mobile, which navigates on click
const DraftCard = ({ draft }: { draft: Draft }) => {
  const router = useRouter();
  const draftTitle: string = getDraftTitle(draft);

  return (
    <div
      onClick={() => router.push(`/editor/edit/${draft.id}`)}
      className="bg-background/50 rounded-lg p-4 mb-4 border border-accent flex flex-col space-y-3 cursor-pointer hover:border-primary transition-colors"
    >
      <div className="flex justify-between items-start">
        <h3 className="text-foreground font-semibold text-base flex-1 pr-2">{draftTitle}</h3>
        <button onClick={(e) => e.stopPropagation()} className="text-accent hover:text-foreground">
          <MoreVertical size={20} />
        </button>
      </div>
      <div className="text-accent text-sm">
        <p><span className="font-medium text-foreground/80">Template:</span> {draft.contentType}</p>
        <p><span className="font-medium text-foreground/80">Created:</span> {formatDate(draft.createdAt)}</p>
      </div>
    </div>
  );
};


// --- Main Drafts Page Component ---
export default function DraftsPage() {
  const router = useRouter();
  const [drafts, setDrafts] = useState<Draft[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const draftsPerPage = 5;

  // Pagination logic
  const indexOfLastDraft = currentPage * draftsPerPage;
  const indexOfFirstDraft = indexOfLastDraft - draftsPerPage;
  const currentDrafts = drafts.slice(indexOfFirstDraft, indexOfLastDraft);
  const totalPages = Math.ceil(drafts.length / draftsPerPage);

  const paginate = (pageNumber: number) => setCurrentPage(pageNumber);

  // Fetch drafts on mount
  useEffect(() => {
    const fetchDrafts = async () => {
      try {
        const statusesToFetch = ['DRAFT'];
        const response = await apiClient.get<Draft[]>('/content', {
          params: {
            statuses: statusesToFetch.join(','),
          },
        });
        setDrafts(response.data);
      } catch (error) {
        console.error('Failed to fetch drafts:', error);
      }
    };

    fetchDrafts();
  }, []);

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
                </tr>
              </thead>
              <tbody>
                {currentDrafts.map((draft) => (
                  <tr
                    key={draft.id}
                    onClick={() => router.push(`/editor/edit/${draft.id}`)}
                    className="border-b border-accent/50 hover:bg-accent/10 transition-colors cursor-pointer"
                  >
                    <td className="p-4 text-foreground font-medium max-w-xs truncate">{getDraftTitle(draft)}</td>
                    <td className="p-4 text-accent">{draft.contentType}</td>
                    <td className="p-4 text-accent">{formatDate(draft.createdAt)}</td>
                    <td className="p-4 text-accent">{draft.status}</td>
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