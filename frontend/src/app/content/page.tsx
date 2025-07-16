'use client';

import React, { useState } from 'react';
import { FileDown, MoreVertical, RefreshCw, CheckCircle, XCircle, ChevronLeft, ChevronRight } from 'lucide-react';

// Mock Data for Jobs
const initialJobs: Job[] = [
  { id: 'job-001', title: 'My girlfriend is mad at me for a stupid reason', template: 'Reddit Story', date: '2024-07-15T10:30:00Z', status: 'Completed' },
  { id: 'job-002', title: 'AITA for telling my brother he needs to grow up?', template: 'Reddit Story', date: '2024-07-15T09:45:00Z', status: 'Processing' },
  { id: 'job-003', title: 'The scariest night of my life in an abandoned house', template: 'Reddit Story', date: '2024-07-14T18:20:00Z', status: 'Error' },
  { id: 'job-004', title: 'How I accidentally became a crypto millionaire', template: 'Reddit Story', date: '2024-07-14T15:00:00Z', status: 'Completed' },
  { id: 'job-005', title: 'TIFU by sending a text to the wrong person', template: 'Reddit Story', date: '2024-07-13T11:55:00Z', status: 'Completed' },
  { id: 'job-006', title: 'My experience with a real-life Karen', template: 'Reddit Story', date: '2024-07-12T22:10:00Z', status: 'Processing' },
  { id: 'job-007', title: 'The best prank I ever pulled on my roommate', template: 'Reddit Story', date: '2024-07-12T14:30:00Z', status: 'Completed' },
  { id: 'job-008', title: 'AITA for eating the last slice of pizza?', template: 'Reddit Story', date: '2024-07-11T08:00:00Z', status: 'Error' },
];

// Helper to format date
interface Job {
  id: string;
  title: string;
  template: string;
  date: string;
  status: 'Completed' | 'Processing' | 'Error';
}

interface StatusBadgeProps {
  status: Job['status'];
}

interface JobCardProps {
  job: Job;
}

const formatDate = (isoString: string): string => {
  const date = new Date(isoString);
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
};

// --- Components ---

// Status Badge Component
const StatusBadge = ({ status } : StatusBadgeProps) => {
  const statusStyles = {
    Completed: {
      bgColor: 'bg-green-500/10',
      textColor: 'text-green-400',
      icon: <CheckCircle className="w-4 h-4" />,
      text: 'Completed',
    },
    Processing: {
      bgColor: 'bg-blue-500/10',
      textColor: 'text-blue-400',
      icon: <RefreshCw className="w-4 h-4 animate-spin" />,
      text: 'Processing',
    },
    Error: {
      bgColor: 'bg-red-500/10',
      textColor: 'text-red-400',
      icon: <XCircle className="w-4 h-4" />,
      text: 'Error',
    },
  };

  const style = statusStyles[status] || {};

  return (
    <div className={`inline-flex items-center gap-x-2 px-3 py-1 rounded-full text-sm font-medium ${style.bgColor} ${style.textColor}`}>
      {style.icon}
      <span>{style.text}</span>
    </div>
  );
};

// Job Card Component for Mobile View
const JobCard = (job: Job) => (
    <div className="bg-gray-800/50 rounded-lg p-4 mb-4 border border-gray-700/50 flex flex-col space-y-3">
        <div className="flex justify-between items-start">
            <h3 className="text-white font-semibold text-base flex-1 pr-2">{job.title}</h3>
            <div className="relative">
                <button className="text-gray-400 hover:text-white">
                    <MoreVertical size={20} />
                </button>
                {/* Dropdown can be implemented here */}
            </div>
        </div>
        <div className="text-gray-400 text-sm">
            <p><span className="font-medium text-gray-300">Template:</span> {job.template}</p>
            <p><span className="font-medium text-gray-300">Created:</span> {formatDate(job.date)}</p>
        </div>
        <div className="flex justify-between items-center pt-2">
            <StatusBadge status={job.status} />
            {job.status === 'Completed' && (
                <button className="flex items-center gap-2 bg-yellow-500 text-black font-bold py-2 px-4 rounded-md hover:bg-yellow-400 transition-colors duration-300">
                    <FileDown size={18} />
                    <span>Download</span>
                </button>
            )}
        </div>
    </div>
);


// Main App Component
export default function ContentPage() {
  const [activePage, setActivePage] = useState('Jobs');
  const [jobs] = useState(initialJobs);
  const [currentPage, setCurrentPage] = useState(1);
  const jobsPerPage = 5;

  const navItems = ['Jobs', 'Drafts', 'Create Video', 'Account'];

  // Pagination logic
  const indexOfLastJob = currentPage * jobsPerPage;
  const indexOfFirstJob = indexOfLastJob - jobsPerPage;
  const currentJobs = jobs.slice(indexOfFirstJob, indexOfLastJob);
  const totalPages = Math.ceil(jobs.length / jobsPerPage);

  const paginate = (pageNumber: number) => setCurrentPage(pageNumber);

  return (
    <div className="bg-background min-h-screen font-sans text-foreground flex flex-col lg:flex-row">
      {/* --- Main Content --- */}
      <main className="flex-1 p-4 sm:p-6 lg:p-8">
        <div className="max-w-7xl mx-auto">
          <header className="mb-8">
            <h2 className="text-3xl font-bold text-foreground tracking-tight">Submitted Jobs</h2>
            <p className="text-accent mt-1">
              Here are the videos you've submitted for generation.
            </p>
          </header>

          {/* --- Jobs List --- */}
          <div className="bg-background border border-foreground rounded-xl shadow-lg">
            {/* Desktop Table */}
            <div className="hidden md:block">
              <table className="w-full text-left">
                <thead className="border-b border-foreground">
                  <tr>
                    <th className="p-4 text-sm font-semibold text-foreground">Title</th>
                    <th className="p-4 text-sm font-semibold text-foreground">Template</th>
                    <th className="p-4 text-sm font-semibold text-foreground">Created</th>
                    <th className="p-4 text-sm font-semibold text-foreground">Status</th>
                    <th className="p-4 text-sm font-semibold text-foreground text-center">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {currentJobs.map((job) => (
                    <tr key={job.id} className="border-b border-foreground hover:bg-gray-800/50 transition-colors duration-200">
                      <td className="p-4 text-foreground font-medium max-w-xs truncate">{job.title}</td>
                      <td className="p-4 text-accent">{job.template}</td>
                      <td className="p-4 text-accent">{formatDate(job.date)}</td>
                      <td className="p-4">
                        <StatusBadge status={job.status} />
                      </td>
                      <td className="p-4 text-center">
                        {job.status === 'Completed' && (
                          <button className="flex items-center justify-center mx-auto gap-2 bg-yellow-500 text-black font-bold py-2 px-4 rounded-md hover:bg-yellow-400 transition-colors duration-300">
                            <FileDown size={18} />
                            <span>Download</span>
                          </button>
                        )}
                        {job.status === 'Error' && (
                           <button className="flex items-center justify-center mx-auto gap-2 bg-gray-600 text-white font-bold py-2 px-4 rounded-md hover:bg-gray-500 transition-colors duration-300">
                            <RefreshCw size={18} />
                            <span>Retry</span>
                          </button>
                        )}
                         {job.status === 'Processing' && (
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
                {currentJobs.map(job => <JobCard key={job.id} job={job} />)}
            </div>
            
            {/* --- Pagination --- */}
            <div className="flex items-center justify-between p-4 border-foreground">
                <button 
                    onClick={() => paginate(currentPage - 1)} 
                    disabled={currentPage === 1}
                    className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-foreground bg-gray-800 rounded-md hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
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
                    className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-foreground bg-gray-800 rounded-md hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    Next
                    <ChevronRight size={16} />
                </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
