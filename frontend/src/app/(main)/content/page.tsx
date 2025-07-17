'use client';

import React, { useEffect, useState } from 'react';
import { FileDown, MoreVertical, RefreshCw, CheckCircle, XCircle, ChevronLeft, ChevronRight } from 'lucide-react';
import apiClient from '@/lib/apiClient';
import { Draft } from '@/types/drafts'; // Import shared types
import { ContentStatus } from '@/types/enums'; // Import shared enums
import { getDraftTitle } from '@/lib/helper'; // Import helper function

// Helper to format date
const formatDate = (isoString: string): string => {
  const date = new Date(isoString);
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
};

// --- Sub-Components ---

// Status Badge Component - Now uses the shared ContentStatus type
const StatusBadge = ({ status }: { status: ContentStatus }) => {
  const statusStyles = {
    // Keys are now uppercase to match the backend enum
    COMPLETED: {
      bgColor: 'bg-green-500/10',
      textColor: 'text-green-400',
      icon: <CheckCircle className="w-4 h-4" />,
      text: 'Completed',
    },
    PROCESSING: {
      bgColor: 'bg-blue-500/10',
      textColor: 'text-blue-400',
      icon: <RefreshCw className="w-4 h-4 animate-spin" />,
      text: 'Processing',
    },
    FAILED: { // Assuming 'Error' status is 'FAILED' in the backend
      bgColor: 'bg-red-500/10',
      textColor: 'text-red-400',
      icon: <XCircle className="w-4 h-4" />,
      text: 'Error',
    },
    DRAFT: { // Added for completeness
      bgColor: 'bg-yellow-500/10',
      textColor: 'text-yellow-400',
      icon: <CheckCircle className="w-4 h-4" />,
      text: 'Draft',
    }
  };

  const style = statusStyles[status] || {};

  return (
    <div className={`inline-flex items-center gap-x-2 px-3 py-1 rounded-full text-sm font-medium ${style.bgColor} ${style.textColor}`}>
      {style.icon}
      <span>{style.text}</span>
    </div>
  );
};

// Job Card for Mobile - Now themed and uses the Draft type
const ContentCard = ({ content }: { content: Draft }) => {
  const draftTitle = getDraftTitle(content);
  return (
    <div className="bg-background/50 rounded-lg p-4 mb-4 border border-accent flex flex-col space-y-3">
        <div className="flex justify-between items-start">
            <h3 className="text-foreground font-semibold text-base flex-1 pr-2">{draftTitle}</h3>
            <div className="relative">
                <button className="text-accent hover:text-foreground">
                    <MoreVertical size={20} />
                </button>
            </div>
        </div>
        <div className="text-accent text-sm">
            <p><span className="font-medium text-foreground/80">Template:</span> {content.contentType}</p>
            <p><span className="font-medium text-foreground/80">Created:</span> {formatDate(content.createdAt)}</p>
        </div>
        <div className="flex justify-between items-center pt-2">
            <StatusBadge status={content.status} />
            {content.status === 'COMPLETED' && (
                <button className="flex items-center gap-2 bg-primary text-white font-bold py-2 px-4 rounded-md hover:bg-primary/90 transition-colors">
                    <FileDown size={18} />
                    <span>Download</span>
                </button>
            )}
        </div>
    </div>
  );
};


// --- Main Content Page Component ---
export default function ContentPage() {
  // 1. Remove mock data, use state for real data
  const [contents, setContents] = useState<Draft[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 5;

  // 2. Add useEffect to fetch data from the API
  useEffect(() => {
    const statusesToFetch = ['PROCESSING', 'COMPLETED', 'FAILED'];
    const fetchContent = async () => {
      try {
        // Fetch content that is NOT a draft (e.g., PROCESSING, COMPLETED, FAILED)
        const response = await apiClient.get<Draft[]>('/content', {
          params: {
            // Pass the statuses as a comma-separated string
            statuses: statusesToFetch.join(','),
          },
        });
        setContents(response.data);
      } catch (error) {
        console.error('Failed to fetch content:', error);
      }
    };
    fetchContent();
  }, []);

  // Pagination logic
  const indexOfLastItem = currentPage * itemsPerPage;
  const indexOfFirstItem = indexOfLastItem - itemsPerPage;
  const currentItems = contents.slice(indexOfFirstItem, indexOfLastItem);
  const totalPages = Math.ceil(contents.length / itemsPerPage);

  const paginate = (pageNumber: number) => setCurrentPage(pageNumber);

  return (
    <main className="flex-1 p-4 sm:p-6 lg:p-8">
      <div className="max-w-7xl mx-auto">
        <header className="mb-8">
          <h2 className="text-3xl font-bold text-foreground tracking-tight">Submitted Content</h2>
          <p className="text-accent mt-1">
            Here are the videos you've submitted for generation.
          </p>
        </header>

        {/* --- Content List Container (themed) --- */}
        <div className="bg-background/50 border border-accent/50 rounded-xl shadow-lg">
          {/* Desktop Table */}
          <div className="hidden md:block">
            <table className="w-full text-left">
              <thead className="border-b border-accent/50">
                <tr>
                  <th className="p-4 text-sm font-semibold text-foreground/80">Title</th>
                  <th className="p-4 text-sm font-semibold text-foreground/80">Template</th>
                  <th className="p-4 text-sm font-semibold text-foreground/80">Submitted</th>
                  <th className="p-4 text-sm font-semibold text-foreground/80">Status</th>
                  <th className="p-4 text-sm font-semibold text-foreground/80 text-center">Actions</th>
                </tr>
              </thead>
              <tbody>
                {currentItems.map((content) => (
                  <tr key={content.id} className="border-b border-accent/50">
                    <td className="p-4 text-foreground font-medium max-w-xs truncate">{getDraftTitle(content)}</td>
                    <td className="p-4 text-accent">{content.contentType}</td>
                    <td className="p-4 text-accent">{formatDate(content.createdAt)}</td>
                    <td className="p-4">
                      <StatusBadge status={content.status} />
                    </td>
                    <td className="p-4 text-center">
                      {content.status === 'COMPLETED' && (
                        <button className="flex items-center justify-center mx-auto gap-2 bg-primary text-white font-bold py-2 px-4 rounded-md hover:bg-primary/90 transition-colors">
                          <FileDown size={18} />
                          <span>Download</span>
                        </button>
                      )}
                      {content.status === 'FAILED' && (
                         <button className="flex items-center justify-center mx-auto gap-2 bg-red-500 text-white font-bold py-2 px-4 rounded-md hover:bg-red-400 transition-colors">
                          <RefreshCw size={18} />
                          <span>Retry</span>
                        </button>
                      )}
                       {content.status === 'PROCESSING' && (
                         <span className="text-accent italic">No actions available</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile Cards */}
          <div className="md:hidden p-4">
              {currentItems.map(content => <ContentCard key={content.id} content={content} />)}
          </div>
          
          {/* Pagination */}
          <div className="flex items-center justify-between p-4">
            <button 
                onClick={() => paginate(currentPage - 1)} 
                disabled={currentPage === 1}
                className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-foreground bg-accent/20 rounded-md hover:bg-accent/30 disabled:opacity-50 disabled:cursor-not-allowed"
            >
                <ChevronLeft size={16} />
                Previous
            </button>
            <span className="text-sm text-accent">
                Page {currentPage} of {totalPages}
            </span>
            <button 
                onClick={() => paginate(currentPage + 1)} 
                disabled={currentPage === totalPages}
                className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-foreground bg-accent/20 rounded-md hover:bg-accent/30 disabled:opacity-50 disabled:cursor-not-allowed"
            >
                Next
                <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}