'use client';

import React, { useState, useEffect, useMemo } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Progress } from "@/components/ui/progress";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import {
  FileVideo,
  Plus,
  Download,
  RefreshCw,
  AlertCircle,
  MoreHorizontal,
  Calendar,
  ChevronLeft,
  ChevronRight,
  Trash2,
  Eye,
  Edit,
  HelpCircle,
} from "lucide-react";

import apiClient from '@/lib/apiClient';
import { Draft } from '@/types/drafts';
import { ContentStatus } from '@/types/content';
import { useNotifications } from '@/context/NotificationContext';
import { getDraftTitle } from '@/lib/helper';
import { useRouter } from 'next/navigation';

// --- Constants ---
const ITEMS_PER_PAGE = 8;

// --- Helper Functions ---
const formatDate = (isoString?: string): string => {
  if (!isoString) return 'N/A';
  return new Date(isoString).toLocaleDateString("en-GB", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
};

const getStatusBadge = (status: ContentStatus) => {
    switch (status) {
        case ContentStatus.PROCESSING:
            return <Badge className="bg-blue-100 text-blue-800 dark:bg-blue-900/50 dark:text-blue-300 border-blue-300/50" variant="outline">Processing</Badge>;
        case ContentStatus.COMPLETED:
            return <Badge className="bg-green-100 text-green-800 dark:bg-green-900/50 dark:text-green-300 border-green-300/50" variant="outline">Completed</Badge>;
        case ContentStatus.FAILED:
            return <Badge variant="destructive">Failed</Badge>;
        case ContentStatus.DRAFT:
            return <Badge variant="outline" className="border-border bg-muted text-muted-foreground hover:bg-muted">Draft</Badge>;
        default:
            return <Badge variant="outline">Unknown</Badge>;
    }
};

// --- Main Page Component ---
export default function ContentLibrary() {
  const [contents, setContents] = useState<Draft[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const router = useRouter();
  
  const { latestVideoStatus } = useNotifications();

  // --- Data Fetching and Real-time Updates ---

  const fetchContent = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const statusesToFetch = ['PROCESSING', 'COMPLETED', 'FAILED', 'DRAFT'];
      const response = await apiClient.get<Draft[]>('/content', {
        params: { statuses: statusesToFetch.join(',') },
      });
      // Sort by most recent first
      const sortedData = response.data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      setContents(sortedData);
    } catch (err) {
      console.error('Failed to fetch content:', err);
      setError('Could not load your content library. Please try again later.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchContent();
  }, []);

  useEffect(() => {
    if (latestVideoStatus) {
      setContents(prevContents =>
        prevContents.map(content =>
          content.id === latestVideoStatus.contentId
            ? { ...content, status: latestVideoStatus.status, progressPercentage: latestVideoStatus.progressPercentage }
            : content
        )
      );
    }
  }, [latestVideoStatus]);

  // --- Memoized Pagination ---
  const paginatedItems = useMemo(() => {
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    return contents.slice(startIndex, startIndex + ITEMS_PER_PAGE);
  }, [contents, currentPage]);
  
  const totalPages = Math.ceil(contents.length / ITEMS_PER_PAGE);

  // --- Action Handlers ---
  const handleDownload = async (item: Draft) => {
      // Create a temporary state to track loading for this specific item
      // This prevents all download buttons from showing a loading state
      const button = document.getElementById(`download-btn-${item.id}`) as HTMLButtonElement | null;
      if (button) {
          button.disabled = true;
          button.innerHTML = '<svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>Downloading...';
      }

      try {
          const response = await apiClient.get<{ url: string }>(`/content/${item.id}/download-url`);
          const presignedUrl = response.data.url;

          // Create a temporary link element to trigger the download
          const link = document.createElement('a');
          link.href = presignedUrl;
          
          // Suggest a filename for the user
          const fileName = `${getDraftTitle(item).replace(/[^a-z0-9]/gi, '_').toLowerCase()}.mp4`;
          link.setAttribute('download', fileName);
          
          // Append to the document, click, and then remove
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);

      } catch (error) {
          console.error("Download failed", error);
          alert("Could not get download link. Please try again later.");
      } finally {
          if (button) {
              button.disabled = false;
              button.innerHTML = '<svg class="h-4 w-4 mr-1" ...>Download</svg>'; // Revert to original content
          }
      }
  };

  const handleGetHelp = (item: Draft) => {
    console.log(`Getting help for failed item: ${item.id}`);
    alert(`To get help with "${getDraftTitle(item)}", please contact support with the ID: ${item.id}`);
  };

  const handleDelete = async (item: Draft) => {
    if (window.confirm(`Are you sure you want to delete "${getDraftTitle(item)}"?`)) {
        console.log(`Deleting: ${item.id}`);
        const originalContents = [...contents];
        // Optimistically remove from UI
        setContents(prev => prev.filter(i => i.id !== item.id));
        try {
            await apiClient.delete(`/content/${item.id}`);
        } catch (error) {
            console.error("Failed to delete content", error);
            // Revert UI change on failure
            setContents(originalContents);
        }
    }
  };
  
  const handleContinue = (item: Draft) => {
      console.log(`Continuing draft: ${item.id}`);
      // Navigate to the editor page with the draft ID
      router.push(`/editor/edit/${item.id}`);
  };

  // --- Render Logic ---

  if (isLoading) {
    return (
      <div className="p-4 md:p-6 lg:p-8 min-h-full">
        <div className="max-w-7xl mx-auto space-y-8">
          <div className="flex justify-between items-center">
            <div>
              <Skeleton className="h-8 w-48 mb-2" />
              <Skeleton className="h-4 w-64" />
            </div>
            <Skeleton className="h-10 w-40" />
          </div>
          <Card>
            <CardHeader>
              <Skeleton className="h-6 w-32" />
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {[...Array(5)].map((_, i) => (
                  <div key={i} className="flex items-center space-x-4 p-2">
                    <Skeleton className="h-12 w-16 rounded-md" />
                    <div className="flex-1 space-y-2">
                      <Skeleton className="h-4 w-3/4" />
                      <Skeleton className="h-3 w-1/2" />
                    </div>
                    <Skeleton className="h-6 w-24 rounded-full" />
                    <Skeleton className="h-8 w-24 rounded-md" />
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 md:p-6 lg:p-8 min-h-full">
        <div className="max-w-7xl mx-auto flex items-center justify-center h-[60vh]">
          <div className="text-center space-y-4">
            <Alert variant="destructive" className="max-w-md mx-auto">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
            <Button onClick={fetchContent} variant="outline">
              <RefreshCw className="h-4 w-4 mr-2" />
              Try Again
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-6 lg:p-8 min-h-full">
      <div className="max-w-7xl mx-auto space-y-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-3xl md:text-4xl font-bold text-foreground">Content Library</h1>
            <p className="text-muted-foreground mt-2">Manage, download, and track all your generated videos.</p>
          </div>
          <Button size="lg" onClick={() => router.push('/create')}>
            <Plus className="h-5 w-5 mr-2" />
            Generate New Video
          </Button>
        </div>

        {/* Content List */}
        <Card className="shadow-sm">
          <CardHeader>
            <CardTitle className="flex items-center">
              <FileVideo className="h-5 w-5 mr-2" />
              Your Videos ({contents.length})
            </CardTitle>
            <CardDescription>
              Track progress, download completed videos, and manage your content.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {contents.length === 0 ? (
                <div className="text-center space-y-6 py-16">
                    <FileVideo className="h-24 w-24 mx-auto text-muted-foreground/30" />
                    <div className="space-y-2">
                        <h2 className="text-2xl font-bold text-foreground">No content yet</h2>
                        <p className="text-muted-foreground max-w-md mx-auto">
                        Start creating amazing videos! Click the button below to generate your first one.
                        </p>
                    </div>
                    <Button size="lg">
                        <Plus className="h-5 w-5 mr-2" />
                        Generate New Video
                    </Button>
                </div>
            ) : (
                <div className="overflow-x-auto">
                    <Table>
                        <TableHeader>
                        <TableRow>
                            <TableHead className="w-[45%]">Video</TableHead>
                            <TableHead>Template</TableHead>
                            <TableHead>Date</TableHead>
                            <TableHead>Status</TableHead>
                            <TableHead className="text-right">Actions</TableHead>
                        </TableRow>
                        </TableHeader>
                        <TableBody>
                        {paginatedItems.map((item) => (
                            <TableRow key={item.id} className="hover:bg-muted/50">
                            <TableCell>
                                <div className="flex items-start space-x-4">
                                  <div className="w-20 h-12 rounded-md bg-muted flex items-center justify-center flex-shrink-0">
                                      <FileVideo className="h-6 w-6 text-muted-foreground" />
                                  </div>
                                <div className="min-w-0 flex-1">
                                    <p className="font-medium text-foreground" title={getDraftTitle(item)}>
                                        {getDraftTitle(item)}
                                    </p>
                                    {item.status === ContentStatus.PROCESSING && typeof item.progressPercentage === 'number' && (
                                    <div className="mt-2 space-y-1.5">
                                        <Progress value={item.progressPercentage} className="h-1.5" />
                                        <p className="text-xs text-muted-foreground">{Math.round(item.progressPercentage)}% complete</p>
                                    </div>
                                    )}
                                </div>
                                </div>
                            </TableCell>
                            <TableCell>
                                <span className="text-muted-foreground">{item.contentType}</span>
                            </TableCell>
                            <TableCell>
                                <div className="flex items-center text-muted-foreground">
                                <Calendar className="h-4 w-4 mr-1.5 flex-shrink-0" />
                                {formatDate(item.createdAt)}
                                </div>
                            </TableCell>
                            <TableCell>{getStatusBadge(item.status)}</TableCell>
                            <TableCell className="text-right">
                                <div className="flex items-center justify-end space-x-2">
                                {item.status === ContentStatus.COMPLETED && (
                                    <Button
                                      id={`download-btn-${item.id}`} // Add unique ID here
                                      size="sm"
                                      onClick={() => handleDownload(item)}
                                    >
                                      <Download className="h-4 w-4 mr-1" />
                                      Download
                                    </Button>
                                )}
                                {item.status === ContentStatus.FAILED && (
                                    <Button size="sm" variant="outline" onClick={() => handleGetHelp(item)}>
                                        <HelpCircle className="h-4 w-4 mr-1" />
                                        Get Help
                                    </Button>
                                )}
                                {item.status === ContentStatus.DRAFT && (
                                    <Button size="sm" variant="outline" onClick={() => handleContinue(item)}>
                                    <Edit className="h-4 w-4 mr-1" />
                                    Continue
                                    </Button>
                                )}
                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                    <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                                        <MoreHorizontal className="h-4 w-4" />
                                    </Button>
                                    </DropdownMenuTrigger>
                                    <DropdownMenuContent align="end">
                                    {item.status !== ContentStatus.FAILED && (
                                        <DropdownMenuItem onClick={() => handleContinue(item)}>
                                            <Eye className="h-4 w-4 mr-2" />
                                            View Details
                                        </DropdownMenuItem>
                                    )}
                                    <DropdownMenuItem
                                        onClick={() => handleDelete(item)}
                                        className="text-red-600 focus:text-red-600"
                                    >
                                        <Trash2 className="h-4 w-4 mr-2" />
                                        Delete
                                    </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                                </div>
                            </TableCell>
                            </TableRow>
                        ))}
                        </TableBody>
                    </Table>
                </div>
            )}
            
            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-6 pt-4 border-t">
                <p className="text-sm text-muted-foreground">
                  Page {currentPage} of {totalPages}
                </p>
                <div className="flex items-center space-x-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCurrentPage(currentPage - 1)}
                    disabled={currentPage === 1}
                  >
                    <ChevronLeft className="h-4 w-4 mr-1" />
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCurrentPage(currentPage + 1)}
                    disabled={currentPage === totalPages}
                  >
                    Next
                    <ChevronRight className="h-4 w-4 ml-1" />
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}