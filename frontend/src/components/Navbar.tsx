import React from 'react';
import { Hotel, Cloud, ShieldCheck } from 'lucide-react';

export const Navbar: React.FC = () => {
  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="bg-emerald-600 p-2 rounded-lg text-white">
            <Hotel className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-lg font-bold text-slate-900 leading-tight">Hotel Cloud-Native</h1>
            <p className="text-xs text-slate-500">Multi-AZ Resilient Architecture</p>
          </div>
        </div>

        <div className="flex items-center space-x-4">
          <div className="hidden md:flex items-center space-x-2 text-xs text-slate-600 bg-slate-100 px-3 py-1.5 rounded-full">
            <Cloud className="w-4 h-4 text-emerald-600" />
            <span>AWS SQS &amp; RDS Multi-AZ</span>
          </div>
          <div className="flex items-center space-x-1.5 text-xs font-medium text-emerald-700 bg-emerald-50 px-2.5 py-1.5 rounded-md border border-emerald-200">
            <ShieldCheck className="w-4 h-4" />
            <span>Stateless API</span>
          </div>
        </div>
      </div>
    </header>
  );
};

